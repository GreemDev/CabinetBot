@file:OptIn(PrivilegedIntent::class)

package net.greemdev.cabinet

import dev.kord.common.entity.*
import dev.kord.core.event.gateway.ReadyEvent
import dev.kord.core.supplier.EntitySupplyStrategy
import dev.kord.gateway.*
import io.ktor.util.logging.*
import kotlinx.coroutines.runBlocking
import net.greemdev.cabinet.extensions.QuestionsExtension
import org.jetbrains.exposed.sql.SchemaUtils
import net.greemdev.cabinet.database.x.*
import net.greemdev.cabinet.database.entities.*
import net.greemdev.cabinet.lib.kordex.*
import net.greemdev.cabinet.lib.meta.onStartup
import net.greemdev.cabinet.lib.util.*

fun main(args: Array<out String>) {
    val sw = Stopwatch.startNew()
    BotConfig.checks()



    runBlocking {
        CabinetBot.init(getCli().handleCommands(args))
    }

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
            use(try {
                botConfig.parseGame()
            } catch (e: IllegalArgumentException) {
                CabinetBot.logger.error(e)
                DiscordBotActivity(botConfig.game, ActivityType.Game)
            }.also {
                CabinetBot.logger.info {
                    string {
                        +"Activity set as ${it.type} \"${it.name}\""
                        if (it.type == ActivityType.Streaming)
                            +", at ${it.url.value}"
                    }
                }
            })
        }

        extensions {
            add(::QuestionsExtension)
        }

        intents {
            +Intent.Guilds
            +Intent.GuildMembers
            +Intent.MessageContent
            +Intent.GuildMessages
            +Intent.GuildMessageReactions
            +Intent.DirectMessages
        }

        kord {
            enableShutdownHook = true
            stackTraceRecovery = true
            defaultStrategy = EntitySupplyStrategy.cacheWithCachingRestFallback
        }
    }

    bot.on<ReadyEvent> {
        onStartup(bot, sw)
    }

    bot.start()
}