package net.greemdev.cabinet.extensions

import com.kotlindiscord.kord.extensions.commands.application.slash.ephemeralSubCommand
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import net.greemdev.cabinet.botConfig
import net.greemdev.cabinet.database.entities.Question
import net.greemdev.cabinet.lib.kordex.CabinetExtension
import net.greemdev.cabinet.lib.kordex.createCheck
import net.greemdev.cabinet.lib.util.markdown
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

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
                            modifyPros {
                                +arguments.pro
                            }
                        }
                    } ?: run {
                        respond {
                            content = "Couldn't find a question with that ID."
                        }
                        return@action
                    }

                    question.updatePostedMessage()

                    respond {
                        content = "Added the benefit ${markdown(arguments.pro).inlineCode()} to question with ID ${arguments.id}"
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
                            modifyCons {
                                +arguments.con
                            }
                        }
                    } ?: run {
                        respond {
                            content = "Couldn't find a question with that ID."
                        }
                        return@action
                    }

                    question.updatePostedMessage()

                    respond {
                        content = "Added the downside ${markdown(arguments.con).inlineCode()} to question with ID ${arguments.id}"
                    }
                }
            }
        }
    }
}