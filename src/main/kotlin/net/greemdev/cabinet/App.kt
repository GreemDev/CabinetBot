@file:OptIn(PrivilegedIntent::class)

package net.greemdev.cabinet

import dev.kord.common.entity.*
import dev.kord.core.event.gateway.ReadyEvent
import dev.kord.core.supplier.EntitySupplyStrategy
import dev.kord.gateway.*
import io.ktor.util.logging.*
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.SchemaUtils
import net.greemdev.cabinet.database.x.*
import net.greemdev.cabinet.database.entities.*
import net.greemdev.cabinet.extensions.procon.ProsAndConsExtension
import net.greemdev.cabinet.extensions.questions.QuestionsExtension
import net.greemdev.cabinet.lib.kordex.*
import net.greemdev.cabinet.lib.onStartup
import net.greemdev.cabinet.lib.util.*

val logger by slf4j<CabinetBot>()

fun main(args: Array<out String>) {
    runBlocking {
        CabinetBot.init(getCli().handleCommands(args))
    }

    val sw = Stopwatch.startNew()
    BotConfig.checks()

    if (!nostart) {
        BotDatabase start {
            SchemaUtils.create(Questions)
        }

        runBlocking(scope.coroutineContext) {
            mainAsync(sw)
        }
    }
}


suspend fun mainAsync(sw: Stopwatch) {
    val bot = buildCabinetBot(botConfig.token) {
        presence {
            val game = with (runCatching(botConfig::parseGame)) {
                when {
                    isSuccess -> getOrThrow()
                    else -> {
                        logger.error(exceptionOrNull()!!)
                        DiscordBotActivity(botConfig.game, ActivityType.Game)
                    }
                }
            }

            logger.info {
                string {
                    +"Activity set as ${game.type} \"${game.name}\""
                    if (game.type == ActivityType.Streaming)
                        +", at ${game.url.value}"
                }
            }

            status = PresenceStatus.Invisible
        }

        extensions {
            add(::QuestionsExtension)
            add(::ProsAndConsExtension)
        }

        intents {
            +Intent.Guilds
            +Intent.MessageContent
            +Intent.GuildMessages
        }

        kord {
            enableShutdownHook = true
            stackTraceRecovery = true
            defaultStrategy = EntitySupplyStrategy.cacheWithCachingRestFallback
        }
    }

    bot.on<ReadyEvent> {
        onStartup(sw)
    }

    bot.start()
}