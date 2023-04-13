package net.greemdev.cabinet.extensions.procon

import com.kotlindiscord.kord.extensions.commands.application.slash.ephemeralSubCommand
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.extensions.event
import dev.kord.core.behavior.edit
import dev.kord.core.event.interaction.SelectMenuInteractionCreateEvent
import dev.kord.rest.builder.message.modify.actionRow
import dev.kord.rest.builder.message.modify.embed
import net.greemdev.cabinet.botConfig
import net.greemdev.cabinet.extensions.arg.*
import net.greemdev.cabinet.database.entities.Question
import net.greemdev.cabinet.lib.kordex.*
import net.greemdev.cabinet.lib.kordex.cacheNonnull
import net.greemdev.cabinet.lib.util.*
import org.jetbrains.exposed.sql.transactions.transaction

const val ProsCommandName = "cabinet-pros"
const val ConsCommandName = "cabinet-cons"
const val AddSubcommandName = "add"
const val RemoveSubcommandName = "remove"
const val AddProCommandName = "$ProsCommandName $AddSubcommandName"
const val AddConCommandName = "$ConsCommandName $AddSubcommandName"
const val RemoveProCommandName = "$ProsCommandName $RemoveSubcommandName"
const val RemoveConCommandName = "$ConsCommandName $RemoveSubcommandName"

class ProsAndConsExtension : CabinetExtension("pros-cons") {
    override suspend fun setup() {
        ephemeralSlashCommand {
            name = ProsCommandName
            description = "Add or remove pros (a positive aspect of a proposed change) for a question."

            ephemeralSubCommand(::AddPro) {
                name = AddSubcommandName
                description = "Add a pro to a question."

                createCheck {
                    if (botConfig.locked)
                        fail("Bot is locked.")
                    val question = cacheNonnullThenGet("question", "Couldn't find a question with that ID.") {
                        transaction {
                            Question.findById(it.interaction.command.integers["vote-id"]!!)
                        }
                    }
                    failIf(question.isImmutable, "That vote is no longer modifiable.")
                }
                action(addPro)
            }
            ephemeralSubCommand(::QuestionArguments) {
                name = RemoveSubcommandName
                description = "Remove a pro from a question."

                createCheck {
                    if (botConfig.locked)
                        fail("Bot is locked.")
                    val question = cacheNonnullThenGet("question", "Couldn't find a question with that ID.") {
                        transaction {
                            Question.findById(it.interaction.command.integers["vote-id"]!!)
                        }
                    }
                    failIf(question.isImmutable, "That vote is no longer modifiable.")
                }
                action(removePro)
            }
        }
        ephemeralSlashCommand {
            name = ConsCommandName
            description = "Add or remove cons (a negative aspect of a proposed change) for question."

            ephemeralSubCommand(::AddCon) {
                name = AddSubcommandName
                description = "Add a con to a question."

                createCheck {
                    failIf(botConfig.locked, "Bot is locked.")
                    val question = cacheNonnullThenGet("question", "Couldn't find a question with that ID.") {
                        transaction {
                            Question.findById(it.interaction.command.integers["vote-id"]!!)
                        }
                    }
                    failIf(question.isImmutable, "That vote is no longer modifiable.")
                }
                action(addCon)
            }
            ephemeralSubCommand(::QuestionArguments) {
                name = RemoveSubcommandName
                description = "Remove a con from a question."

                createCheck {
                    failIf(botConfig.locked, "Bot is locked.")
                    val question = cacheNonnullThenGet("question", "Couldn't find a question with that ID.") {
                        transaction {
                            Question.findById(it.interaction.command.integers["vote-id"]!!)
                        }
                    }
                    failIf(question.isImmutable, "That vote is no longer modifiable.")
                }
                action(removeCon)
            }
        }

        event<SelectMenuInteractionCreateEvent> {
            createCheck {
                failIf(!event.interaction.user.asMember(botConfig.guild).isCabinetMember, "You are not a member of the Cabinet.")
            }
            action {
                if (event.interaction.componentId.startsWith("remove-pro")) {
                    val vote = event.interaction.componentId.split(':').last().toLong()
                    val question = transaction {
                        Question.findById(vote)!!.apply {
                            modifyPros {
                                -event.interaction.values
                            }
                        }
                    }

                    if (question.isImmutable) {
                        event.interaction.message.edit {
                            embeds = mutableListOf()
                            content = "That vote is no longer modifiable."
                        }
                    } else {
                        event.interaction.message.edit {
                            embed {
                                title = "Please select one or multiple options below to remove."
                            }
                            actionRow { prosSelect(question) }
                        }
                        question.updatePostedMessage()
                    }
                }

                if (event.interaction.componentId.startsWith("remove-con")) {
                    val vote = event.interaction.componentId.split(':').last().toLong()
                    val question = transaction {
                        Question.findById(vote)!!.apply {
                            modifyCons {
                                -event.interaction.values
                            }
                        }
                    }
                    if (question.isImmutable) {
                        event.interaction.message.edit {
                            embeds = mutableListOf()
                            content = "That vote is no longer modifiable."
                        }
                    } else {
                        event.interaction.message.edit {
                            embed {
                                title = "Please select one or multiple options below to remove."
                            }
                            actionRow { consSelect(question) }
                        }
                        question.updatePostedMessage()
                    }
                }
            }
        }
    }
}