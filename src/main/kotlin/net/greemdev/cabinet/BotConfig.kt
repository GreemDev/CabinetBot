package net.greemdev.cabinet

import dev.kord.common.entity.ActivityType
import dev.kord.common.entity.DiscordBotActivity
import dev.kord.common.entity.Snowflake
import dev.kord.common.entity.optional.Optional
import net.greemdev.cabinet.lib.util.*
import java.io.File
import kotlinx.serialization.*
import net.greemdev.cabinet.lib.util.parse.ColorParser
import kotlin.IllegalArgumentException
import kotlin.jvm.Throws
import kotlin.system.exitProcess

private val clogger by slf4j("Config")
val botConfig by invoking { BotConfig.get().orNull() ?: error("no config available") }

private const val fileloc = "data/config.json"

@Serializable
data class BotConfig(
    val locked: Boolean,
    val token: String,
    val guild: Snowflake,
    val cabinetChannel: Snowflake,
    val cabinetRole: Snowflake,
    val presidentRole: Snowflake,
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
            defaultGuildValue.snowflake,
            defaultCabinetChannelValue.snowflake,
            defaultCabinetRoleValue.snowflake,
            defaultPresidentRoleValue.snowflake,
            defaultGameValue,
            defaultOwnerIdValue,
            defaultEmbedColorValue,
            listOf()
        )

        init {
            if (File("data").mkdir())
                file().createNewFile()
        }

        fun checks() {
            val f = file()
            if (!f.exists()) {
                write()
                clogger.warn("Please fill in the config.json config file in the data folder, and restart me!")
                exitProcess(-1)
            }

            if (f.readText().isEmpty())
                write()

            get() // has a color check in it since it's dynamically loaded
        }

        fun file() = File(fileloc).also {
            if (it.exists()) {
                if (!it.isReadable)
                    it.isReadable = true
                if (!it.isWritable)
                    it.isWritable = true
            }
        }

        fun write(config: BotConfig = BotConfig()) {
            file().writeText(formatJsonString(config, pretty = true))
        }

        fun get() =
            runCatching {
                try {
                    parseJsonString<BotConfig>(file().readText(), true)
                } catch (e: Exception) {
                    e.printStackTrace()
                    throw e
                }
            }.opt().also { opt ->
                opt.ifPresent {
                    val cpr = ColorParser.tryParse(it.embedColor)
                    cpr.exceptionOrNull()?.let { t ->
                        clogger.error("Invalid color defined", t)
                        clogger.warn("Please fix the above issue and restart.")
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
private const val defaultGuildValue = 858547359804555264
private const val defaultCabinetChannelValue = 858791066147618857
private const val defaultCabinetRoleValue = 900431261015879721
private const val defaultPresidentRoleValue = 858548175711240192
private const val defaultGameValue = "your-game-here"
private const val defaultOwnerIdValue = "your-id-here"
private const val defaultEmbedColorValue = "#7000FB"