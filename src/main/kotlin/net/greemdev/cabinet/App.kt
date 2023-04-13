@file:OptIn(PrivilegedIntent::class)

package net.greemdev.cabinet

import dev.kord.common.entity.*
import dev.kord.common.kColor
import dev.kord.core.event.gateway.ReadyEvent
import dev.kord.core.supplier.EntitySupplyStrategy
import dev.kord.gateway.*
import dev.kord.rest.builder.message.create.embed
import io.ktor.util.logging.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.SchemaUtils
import net.greemdev.cabinet.database.x.*
import net.greemdev.cabinet.database.entities.*
import net.greemdev.cabinet.extensions.procon.ProsAndConsExtension
import net.greemdev.cabinet.extensions.questions.QuestionsExtension
import net.greemdev.cabinet.lib.kordex.*
import net.greemdev.cabinet.lib.onStartup
import net.greemdev.cabinet.lib.util.*
import org.koin.core.instance.InstanceFactory
import org.koin.mp.KoinPlatformTools
import java.awt.Color

val logger by slf4j<CabinetBot>()

fun main(args: Array<out String>) {
    runBlocking {
        CabinetBot.init(CLI.handleCommands(args))
    }

    val sw = Stopwatch.startNew()
    BotConfig.checks()

    if (!nostart) {
        BotDatabase {
            SchemaUtils.create(Questions)
        }

        runBlocking(scope.coroutineContext) {
            mainAsync(sw)
        }
    }
}


suspend fun mainAsync(sw: Stopwatch) {
    val conf = botConfig

    val bot = buildCabinetBot(conf.token) {
        presence {
            val game = with (runCatching(conf::parseGame)) {
                when {
                    isSuccess -> getOrThrow()
                    else -> {
                        logger.error(exceptionOrNull()!!)
                        DiscordBotActivity(conf.game, ActivityType.Game)
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

        errorResponse { message, reason ->
            if (reason.error is Fail) {
                content = message
                return@errorResponse
            }

            fun formatStackTrace(t: Throwable) =
                string {
                    val separator = InstanceFactory.ERROR_SEPARATOR
                    +t.toString()
                    +separator

                    val (first25, theRest) = t.stackTrace
                        .takeWhile { "sun.reflect" !in it.className }
                        .let {
                            it.take(25) to it.drop(25)
                        }

                    +first25.joinToString(separator)

                    if (theRest.isNotEmpty()) {
                        +separator
                        +"... and ${theRest.size} more"
                    }
                }

            embed {
                color = Color.RED.kColor
                title = "Internal error | Please copy & send this to Greem#1337!"
                description = markdown(formatStackTrace(reason.error)).blockCode()
            }
        }

        extensions {
            add(::QuestionsExtension)
            add(::ProsAndConsExtension)
        }

        intents {
            +Intent.Guilds
            +Intent.MessageContent
            +Intent.GuildMessages
            +Intent.GuildMembers
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