package net.greemdev.cabinet.lib.util

import dev.kord.common.Color
import dev.kord.common.entity.ActivityType
import dev.kord.common.entity.DiscordBotActivity
import dev.kord.common.entity.Snowflake
import dev.kord.common.kColor
import dev.kord.core.behavior.UserBehavior
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.entity.Member
import dev.kord.core.entity.Role
import dev.kord.core.entity.User
import dev.kord.core.kordLogger
import dev.kord.gateway.builder.PresenceBuilder
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.builder.message.create.UserMessageCreateBuilder
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onEach
import kotlinx.datetime.Instant
import net.greemdev.cabinet.botConfig
import net.greemdev.cabinet.lib.util.parse.ColorParser

val Member.highestRole: Role
    get() {
        var role: Role? = null
        roles.onEach {
            if (it.rawPosition > (role?.rawPosition ?: -1))
                role = it
        }.catch {
            kordLogger.catching(it)
        }
        return role ?: error("User has no roles.")
    }

infix fun EmbedBuilder.colorOf(member: Member?) {
    color = tryOrNull { member?.highestRole }?.color
}

fun EmbedBuilder.colorOfOrDefault(member: Member?) {
    color = tryOrNull { member?.highestRole }?.color ?: Color.embedDefault
}

val User.effectiveAvatar
    get() = avatar ?: defaultAvatar

suspend fun UserBehavior.createMessageOrNull(content: String) = getDmChannelOrNull()?.createMessage(content)
suspend fun UserBehavior.createMessageOrNull(builder: suspend UserMessageCreateBuilder.() -> Unit) = getDmChannelOrNull()?.createMessage { builder() }
suspend fun UserBehavior.createEmbedOrNull(builder: suspend EmbedBuilder.() -> Unit) = getDmChannelOrNull()?.createEmbed { builder() }

val Color.Companion.embedDefault by invoking { ColorParser.unsafeParse(botConfig.defaultEmbedColor).kColor }

fun PresenceBuilder.use(activity: DiscordBotActivity) {
    when (activity.type) {
        ActivityType.Game -> playing(activity.name)
        ActivityType.Listening -> listening(activity.name)
        ActivityType.Watching -> watching(activity.name)
        ActivityType.Competing -> competing(activity.name)
        ActivityType.Streaming -> streaming(activity.name, activity.url.value!!)
        else -> error("How did this happen")
    }
}

val Long.snowflake: Snowflake
    get() = Snowflake(this)

val ULong.snowflake: Snowflake
    get() = Snowflake(this)

val String.asSnowflake: Snowflake
    get() = Snowflake(this)

fun markdown(value: String) = object : Markdown(value) {}
fun markdown(value: Instant) = markdown(value.toEpochMilliseconds().string())

abstract class Markdown internal constructor(private var value: String) {
    private fun String.surround(with: Any) = "$with$this$with"

    fun bold() = value.surround("**")
    fun boldItalicize() = value.surround("***")
    fun italicize() = value.surround('*')
    fun spoiler() = value.surround("||")
    fun underline() = value.surround("__")
    infix fun maskedUrl(url: String) = "[$value]($url)"
    fun hideUrlEmbed() = "<$value>"
    fun inlineCode() = value.surround('`')
    fun blockCode(lang: String = "") = "${if (lang.isNotEmpty()) "$lang\n" else ""}${value}".surround("```")
    infix fun timestamp(type: TimestampType) = if (value.isNumeric()) {
        Instant.fromEpochMilliseconds(value.toLong()).asDiscordTimestamp(type)
    } else error("cannot create a timestamp with a non-numeric value")
}

enum class TimestampType {
    ShortTime,
    LongTime,
    ShortDate,
    LongDate,
    ShortDateTime,
    LongDateTime,
    Relative;

    override fun toString() = when (this) {
        ShortTime -> "t"
        LongTime -> "T"
        ShortDate -> "d"
        LongDate -> "D"
        ShortDateTime -> "f"
        LongDateTime -> "F"
        Relative -> "R"
    }
}

fun Instant.asDiscordTimestamp(type: TimestampType) = string {
    +"<t:"
    +"$epochSeconds"
    +':'
    +type
    +'>'
}