@file:Suppress("RemoveExplicitTypeArguments")

package net.greemdev.cabinet.extensions

import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.EphemeralSlashCommandContext
import com.kotlindiscord.kord.extensions.commands.application.slash.PublicSlashCommandContext
import com.kotlindiscord.kord.extensions.commands.application.slash.SlashCommandContext
import com.kotlindiscord.kord.extensions.components.forms.ModalForm
import com.kotlindiscord.kord.extensions.types.respond

typealias EphemeralCommandWithModal<A, M> = suspend EphemeralSlashCommandContext<A, M>.(M?) -> Unit
typealias EphemeralCommandWithRequiredModal<A, M> = suspend EphemeralSlashCommandContext<A, M>.(M) -> Unit
typealias EphemeralCommand<A, M> = suspend EphemeralSlashCommandContext<A, M>.() -> Unit
typealias PublicCommandWithModal<A, M> = suspend PublicSlashCommandContext<A, M>.(M?) -> Unit
typealias PublicCommandWithRequiredModal<A, M> = suspend PublicSlashCommandContext<A, M>.(M?) -> Unit
typealias PublicCommand<A, M> = suspend PublicSlashCommandContext<A, M>.() -> Unit

const val MissingModalMessage = "Fill in the popup! Run the command again!"

// ephemeral commands
fun<A : Arguments, M : ModalForm> ephemeralCommand(
    body: EphemeralCommandWithModal<A, M>
)= lazy { ecmd<A, M>(body).action }

fun<A : Arguments, M : ModalForm> ephemeralCommandRequireModal(
    body: EphemeralCommandWithRequiredModal<A, M>
)= lazy {
    ecmd<A, M> {
        it?.let { body(it) } ?: respond { content = MissingModalMessage }
    }.action
}

fun<A : Arguments> ephemeralCommand(
    body: EphemeralCommand<A, ModalForm>
) = lazy { ecmd<A, ModalForm> { body() }.action }

fun<M : ModalForm> ephemeralCommandModalOnly(
    body: EphemeralCommandWithModal<Arguments, M>
) = lazy { ecmd<Arguments, M>(body).action }

fun<M : ModalForm> ephemeralCommandRequireModalOnly(
    body: EphemeralCommandWithRequiredModal<Arguments, M>
) = lazy {
   ecmd<Arguments, M> {
       it?.let { body(it) } ?: respond { content = MissingModalMessage }
    }.action
}

// public commands
fun<A : Arguments, M : ModalForm> publicCommand(
    body: PublicCommandWithModal<A, M>
) = lazy { pcmd<A, M>(body).action }

fun<A : Arguments, M : ModalForm> publicCommandRequireModal(
    body: PublicCommandWithRequiredModal<A, M>
) = lazy {
    pcmd<A, M> {
        it?.let { body(it) } ?: respond { content = MissingModalMessage }
    }.action
}



fun<A : Arguments> publicCommand(
    body: PublicCommand<A, ModalForm>
) = lazy {
    pcmd<A, ModalForm> {
        body()
    }.action
}

fun<M : ModalForm> publicCommandOnlyModal(
    body: PublicCommandWithModal<Arguments, M>
) = lazy { pcmd<Arguments, M>(body).action }

fun<M : ModalForm> publicCommandRequireModalOnly(
    body: PublicCommandWithRequiredModal<Arguments, M>
) = lazy {
    pcmd<Arguments, M> {
        it?.let { body(it) } ?: respond { content = MissingModalMessage }
    }.action
}

@Suppress("SpellCheckingInspection") //internal
private fun<A : Arguments, M : ModalForm> ecmd(
    action: EphemeralCommandWithModal<A, M>
) = object : BotCommand<EphemeralSlashCommandContext<A, M>, A, M>(action) {}

@Suppress("SpellCheckingInspection") //internal
private fun<A : Arguments, M : ModalForm> pcmd(
    action: PublicCommandWithModal<A, M>
) = object : BotCommand<PublicSlashCommandContext<A, M>, A, M>(action) {}

private abstract class BotCommand<C : SlashCommandContext<C, A, M>, A : Arguments, M : ModalForm>(
    val action: suspend C.(M?) -> Unit
)