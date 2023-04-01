package net.greemdev.cabinet.lib.kordex

import com.kotlindiscord.kord.extensions.checks.types.CheckContext
import com.kotlindiscord.kord.extensions.checks.types.CheckContextWithCache
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.SlashCommand
import com.kotlindiscord.kord.extensions.commands.application.slash.SlashCommandContext
import com.kotlindiscord.kord.extensions.commands.chat.ChatCommand
import com.kotlindiscord.kord.extensions.components.forms.ModalForm
import com.kotlindiscord.kord.extensions.events.EventHandler
import dev.kord.core.event.Event
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import net.greemdev.cabinet.lib.util.optionalOf
import java.util.*

class Fail private constructor(message: String?, cause: Throwable? = null) : Throwable(message, cause) {
    companion object {
        fun new(message: String?, cause: Throwable? = null): Fail = Fail(message, cause)
        operator fun invoke(message: String?, cause: Throwable? = null): Nothing {
            throw Fail(message, cause)
        }
    }
}

abstract class CheckCreateScope<T : Event>(val cacheContext: CheckContextWithCache<T>) {

    val event = cacheContext.event


    fun<V> Optional<V>.orElseFail(message: String? = null): V = orElseThrow { Fail.new(message) }

    fun fail(message: String? = null): Nothing = Fail(message)
    fun failIf(condition: Boolean, message: String? = null) {
        if (condition)
            fail(message)
    }
    fun failIfNot(condition: Boolean, message: String? = null) = failIf(!condition, message)
}

fun<T : Event> EventHandler<T>.createCheck(block: suspend CheckCreateScope<T>.() -> Unit) {
    check {
        this.createCheck(block)
    }
}

fun<C : SlashCommandContext<*, A, M>, A : Arguments, M : ModalForm> SlashCommand<C, A, M>.createCheck(block: suspend CheckCreateScope<ChatInputCommandInteractionCreateEvent>.() -> Unit) {
    check {
        this.createCheck(block)
    }
}

fun<A : Arguments> ChatCommand<A>.createCheck(block: suspend CheckCreateScope<MessageCreateEvent>.() -> Unit) {
    check {
        this.createCheck(block)
    }
}

suspend fun<T : Event> CheckContextWithCache<T>.createCheck(block: suspend CheckCreateScope<T>.() -> Unit) {
    val scope = object : CheckCreateScope<T>(this) {}
    try {
        scope.block()
    } catch (f: Fail) {
        fail(f.message)
    }
    pass()
}