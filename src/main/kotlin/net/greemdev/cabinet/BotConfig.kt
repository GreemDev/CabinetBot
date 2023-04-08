package net.greemdev.cabinet

import dev.kord.common.entity.ActivityType
import dev.kord.common.entity.DiscordBotActivity
import dev.kord.common.entity.Snowflake
import dev.kord.common.entity.optional.Optional
import net.greemdev.cabinet.lib.util.*
import java.io.File
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import net.greemdev.cabinet.lib.util.parse.ColorParser
import kotlin.IllegalArgumentException
import kotlin.jvm.Throws
import kotlin.system.exitProcess

val botConfig by invoking { BotConfig.get().orNull() ?: error("no config available") }

private const val fileloc = "data/config.json"

@Serializable
data class BotConfig(
    val locked: Boolean,
    val token: String,
    val cabinetChannel: Snowflake,
    val game: String,
    val ownerId: String,
    val embedColor: String,
    val autoAbstain: List<Snowflake>
) {
    companion object {
        // Pseudo-constructor providing an instance of [BotConfig] with the default values defined at the bottom of this file
        operator fun invoke() = BotConfig(
            defaultLockedValue,
            defaultTokenValue,
            defaultCabinetChannelValue.snowflake,
            defaultGameValue,
            defaultOwnerIdValue,
            defaultEmbedColorValue,
            listOf()
        )

        init {
            if (File("data").mkdir())
                file().createNewFile()
        }

        private val logger by slf4j("Config")

        fun checks() {
            val f = file()
            if (!f.exists()) {
                write()
                logger.warn("Please fill in the config.json config file in the data folder, and restart me!")
                exitProcess(-1)
            }

            if (f.readText().isEmpty())
                write()

            get() // has a color check in it since it's dynamically loaded
        }

        fun file() = File(fileloc).also {
            if (it.exists()) {
                it.setReadable(true)
                it.setWritable(true)
            }
        }

        fun write(config: BotConfig = BotConfig()) {
            file().writeText(formatJsonString(config, pretty = true))
        }

        fun get(): java.util.Optional<BotConfig> =
            optionalOf(try {
                Json.decodeFromString<BotConfig>(file().readText())
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }).also { opt ->
                opt.ifPresent {
                    val cpr = ColorParser.tryParse(it.embedColor)
                    cpr.exceptionOrNull()?.let { t ->
                        logger.error("Invalid color defined", t)
                        logger.warn("Please fix the above issue and restart.")
                        exitProcess(-1)
                    }
                }
            }
    }

    @Throws(IllegalArgumentException::class)
    fun parseGame(): DiscordBotActivity {
        val parts = game.split(" ")
        return if (parts.size == 1) {
            DiscordBotActivity(game, ActivityType.Game)
        } else if (game.startsWith("stream", true) || game.startsWith("streaming", true)) {
            if (parts.size < 3)
                DiscordBotActivity(parts.drop(1).joinToString(" "), ActivityType.Game)
            else {
                val rest = parts.drop(2).joinToString(" ")
                if (parts[1].startsWith("http")) {
                    if ("://twitch.tv/" in parts[1]) {
                        DiscordBotActivity(rest, ActivityType.Streaming, Optional(parts[1]))
                    } else
                        DiscordBotActivity(game, ActivityType.Game).also {
                            logger.warn { "Cannot use a non-twitch URL as the stream URL. Either use a Twitch username, or a full twitch.tv URL. Using default activity type." }
                        }
                } else DiscordBotActivity(rest, ActivityType.Streaming, Optional("https://twitch.tv/${parts[1]}"))
            }
        } else if (game.startsWith("compete", true) || game.startsWith("competingin", true))
            DiscordBotActivity(parts.drop(1).joinToString(" "), ActivityType.Competing)
        else if (game.startsWith("watch", true) || game.startsWith("watching", true))
            DiscordBotActivity(parts.drop(1).joinToString(" "), ActivityType.Watching)
        else if (game.startsWith("listen", true) || game.startsWith("listening", true) || game.startsWith("listeningto", true))
            DiscordBotActivity(parts.drop(1).joinToString(" "), ActivityType.Listening)
        else DiscordBotActivity(game, ActivityType.Game)
    }
}

private const val defaultLockedValue = true
private const val defaultTokenValue = "your-token-here"
private const val defaultCabinetChannelValue = 858791066147618857u
private const val defaultGameValue = "your-game-here"
private const val defaultOwnerIdValue = "your-id-here"
private const val defaultEmbedColorValue = "#7000FB"