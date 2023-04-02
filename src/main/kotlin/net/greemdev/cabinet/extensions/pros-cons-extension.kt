package net.greemdev.cabinet.extensions

import com.kotlindiscord.kord.extensions.commands.application.slash.ephemeralSubCommand
import com.kotlindiscord.kord.extensions.components.menus.SelectMenu
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.extensions.event
import com.kotlindiscord.kord.extensions.types.editingPaginator
import com.kotlindiscord.kord.extensions.types.respond
import dev.kord.common.Color
import dev.kord.core.behavior.edit
import dev.kord.core.entity.component.StringSelectComponent
import dev.kord.core.event.interaction.ComponentInteractionCreateEvent
import dev.kord.core.event.interaction.SelectMenuInteractionCreateEvent
import dev.kord.rest.builder.component.option
import dev.kord.rest.builder.message.create.actionRow
import dev.kord.rest.builder.message.create.embed
import dev.kord.rest.builder.message.modify.actionRow
import dev.kord.rest.builder.message.modify.embed
import net.greemdev.cabinet.botConfig
import net.greemdev.cabinet.database.entities.Question
import net.greemdev.cabinet.lib.kordex.CabinetBot
import net.greemdev.cabinet.lib.kordex.CabinetExtension
import net.greemdev.cabinet.lib.kordex.createCheck
import net.greemdev.cabinet.lib.util.embedDefault
import net.greemdev.cabinet.lib.util.isCabinetMember
import net.greemdev.cabinet.lib.util.markdown
import net.greemdev.cabinet.lib.util.snowflake
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

const val ProsCommandName = "cabinet-pros"
const val ConsCommandName = "cabinet-cons"
const val AddProCommandName = "$ProsCommandName add"
const val AddConCommandName = "$ConsCommandName add"
const val RemoveProCommandName = "$ProsCommandName add"
const val RemoveConCommandName = "$ConsCommandName add"

class ProsAndConsExtension : CabinetExtension("pros-cons") {
    override suspend fun setup() {
        ephemeralSlashCommand {
            name = ProsCommandName
            description = "Add or remove pros (a positive aspect of a proposed change) for a question."
            ephemeralSubCommand(CommandArgs.ProsCons::AddPro) {
                name = "add"
                description = "Add a pro to a question."
                createCheck {
                    if (botConfig.locked)
                        fail("Bot is locked.")
                }
                action {
                    val question = newSuspendedTransaction {
                        Question.findById(arguments.id.toLong())?.apply {
                            if (!isImmutable && arguments.pro.length <= 100) {
                                modifyPros {
                                    +arguments.pro
                                }
                            }
                        }
                    } ?: run {
                        respond {
                            content = "Couldn't find a question with that ID."
                        }
                        return@action
                    }

                    if (arguments.pro.length > 100) {
                        respond {
                            embed {
                                color = Color.embedDefault
                                title = "That was too long."
                                description = "Please limit the input to 100 characters; your input was ${arguments.pro.length - 100} too long. The input is the raw content of this message so you can copy it easily."
                            }
                            content = arguments.pro
                        }
                        return@action
                    }

                    if (!question.isImmutable) {
                        question.updatePostedMessage()

                        respond {
                            content = "Added the benefit ${markdown(arguments.pro).inlineCode()} to question with ID ${arguments.id}"
                        }
                    } else {
                        respond {
                            content = "That vote is no longer modifiable."
                        }
                    }
                }
            }
            ephemeralSubCommand(CommandArgs::ProsCons) {
                name = "remove"
                description = "Remove a pro from a question."
                createCheck {
                    if (botConfig.locked)
                        fail("Bot is locked.")
                }
                action {
                    val question = newSuspendedTransaction {
                        Question.findById(arguments.id.toLong())
                    } ?: run {
                        respond {
                            content = "Couldn't find a question with that ID."
                        }
                        return@action
                    }


                    if (question.isImmutable) {
                        respond {
                            content = "That vote is no longer modifiable."
                        }
                    }
                    respond {
                        embed {
                            title = "Please select one or multiple options below to remove."
                        }
                        actionRow { question.prosSelectMenu() }
                    }
                }
            }
        }
        ephemeralSlashCommand {
            name = ConsCommandName
            description = "Add or remove cons (a negative aspect of a proposed change) for question."
            ephemeralSubCommand(CommandArgs.ProsCons::AddCon) {
                name = "add"
                description = "Add a con to a question."
                createCheck {
                    if (botConfig.locked)
                        fail("Bot is locked.")
                }
                action {
                    val question = newSuspendedTransaction {
                        Question.findById(arguments.id.toLong())?.apply {
                            if (!isImmutable && arguments.con.length <= 100) {
                                modifyCons {
                                    +arguments.con
                                }
                            }
                        }
                    } ?: run {
                        respond {
                            content = "Couldn't find a question with that ID."
                        }
                        return@action
                    }

                    if (arguments.con.length > 100) {
                        respond {
                            embed {
                                color = Color.embedDefault
                                title = "That was too long."
                                description = "Please limit the input to 100 characters; your input was ${arguments.con.length - 100} too long. The input is the raw content of this message so you can copy it easily."
                            }
                            content = arguments.con
                        }
                        return@action
                    }

                    if (!question.isImmutable) {
                        question.updatePostedMessage()

                        respond {
                            content = "Added the downside ${markdown(arguments.con).inlineCode()} to question with ID ${arguments.id}"
                        }
                    } else {
                        respond {
                            content = "That vote is no longer modifiable."
                        }
                    }
                }
            }
            ephemeralSubCommand(CommandArgs::ProsCons) {
                name = "remove"
                description = "Remove a con from a question."
                createCheck {
                    if (botConfig.locked)
                        fail("Bot is locked.")
                }
                action {
                    val question = newSuspendedTransaction {
                        Question.findById(arguments.id.toLong())
                    } ?: run {
                        respond {
                            content = "Couldn't find a question with that ID."
                        }
                        return@action
                    }


                    if (question.isImmutable) {
                        respond {
                            content = "That vote is no longer modifiable."
                        }
                    }
                    respond {
                        embed {
                            title = "Please select one or multiple options below to remove."
                        }
                        actionRow { question.consSelectMenu() }
                    }
                }
            }
        }

        event<SelectMenuInteractionCreateEvent> {
            createCheck {
                failIf(!event.interaction.user.asMember(CabinetBot.prismSmpId.snowflake).isCabinetMember, "You are not a member of the Cabinet.")
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
                    event.interaction.message.edit {
                        embed {
                            title = "Please select one or multiple options below to remove."
                        }
                        actionRow { question.prosSelectMenu() }
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
                    event.interaction.message.edit {
                        embed {
                            title = "Please select one or multiple options below to remove."
                        }
                        actionRow { question.consSelectMenu() }
                    }
                }
            }
        }
    }
}