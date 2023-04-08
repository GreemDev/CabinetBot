package net.greemdev.cabinet.extensions

import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.EphemeralSlashCommandContext
import com.kotlindiscord.kord.extensions.commands.application.slash.PublicSlashCommandContext
import com.kotlindiscord.kord.extensions.commands.application.slash.SlashCommandContext
import com.kotlindiscord.kord.extensions.components.forms.ModalForm
import com.kotlindiscord.kord.extensions.types.respond

const val MissingModalMessage = "Fill in the popup! Run the command again!"

// ephemeral commands
fun<A : Arguments, M : ModalForm> ephemeralCommand(
    action: suspend EphemeralSlashCommandContext<A, M>.(M?) -> Unit
)= lazy {
    object : BotCommand<EphemeralSlashCommandContext<A, M>, A, M>({
        action(it)
    }) {}.action
}

fun<A : Arguments, M : ModalForm> ephemeralCommandRequireModal(
    action: suspend EphemeralSlashCommandContext<A, M>.(M) -> Unit
)= lazy {
    object : BotCommand<EphemeralSlashCommandContext<A, M>, A, M>({
        if (it == null) {
            respond {
                content = MissingModalMessage
            }
        } else action(it)
    }) {}.action
}

fun<A : Arguments> ephemeralCommand(
    action: suspend EphemeralSlashCommandContext<A, ModalForm>.() -> Unit
) = lazy {
    object : BotCommand<EphemeralSlashCommandContext<A, ModalForm>, A, ModalForm>({
        action()
    }) {}.action
}

fun<M : ModalForm> ephemeralCommand(
    action: suspend EphemeralSlashCommandContext<Arguments, M>.(M?) -> Unit
) = lazy {
    object : BotCommand<EphemeralSlashCommandContext<Arguments, M>, Arguments, M>({
        action(it)
    }) {}.action
}

fun<M : ModalForm> ephemeralCommandRequireModal(
    action: suspend EphemeralSlashCommandContext<Arguments, M>.(M) -> Unit
) = lazy {
    object : BotCommand<EphemeralSlashCommandContext<Arguments, M>, Arguments, M>({
        if (it == null) {
            respond {
                content = MissingModalMessage
            }
        } else action(it)
    }) {}.action
}

// public commands
fun<A : Arguments, M : ModalForm> publicCommand(
    action: suspend PublicSlashCommandContext<A, M>.(M?) -> Unit
) = lazy {
    object : BotCommand<PublicSlashCommandContext<A, M>, A, M>({
        action(it)
    }) {}.action
}

fun<A : Arguments, M : ModalForm> publicCommandRequireModal(
    action: suspend PublicSlashCommandContext<A, M>.(M) -> Unit
) = lazy {
    object : BotCommand<PublicSlashCommandContext<A, M>, A, M>({
        if (it == null) {
            respond {
                content = MissingModalMessage
            }
        } else action(it)
    }) {}.action
}



fun<A : Arguments> publicCommand(
    action: suspend PublicSlashCommandContext<A, ModalForm>.() -> Unit
) = lazy {
    object : BotCommand<PublicSlashCommandContext<A, ModalForm>, A, ModalForm>({
        action()
    }) {}.action
}

fun<M : ModalForm> publicCommand(
    action: suspend PublicSlashCommandContext<Arguments, M>.(M?) -> Unit
) = lazy {
    object : BotCommand<PublicSlashCommandContext<Arguments, M>, Arguments, M>({
        action(it)
    }) {}.action
}

fun<M : ModalForm> publicCommandRequireModal(
    action: suspend PublicSlashCommandContext<Arguments, M>.(M) -> Unit
) = lazy {
    object : BotCommand<PublicSlashCommandContext<Arguments, M>, Arguments, M>({
        if (it == null) {
            respond {
                content = MissingModalMessage
            }
        } else action(it)
    }) {}.action
}

private abstract class BotCommand<C : SlashCommandContext<C, A, M>, A : Arguments, M : ModalForm>(
    val action: suspend C.(M?) -> Unit
)