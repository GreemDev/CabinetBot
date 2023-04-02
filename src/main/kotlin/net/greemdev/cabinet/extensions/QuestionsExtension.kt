package net.greemdev.cabinet.extensions

import com.kotlindiscord.kord.extensions.components.forms.ModalForm
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.rest.builder.message.create.embed
import dev.kord.x.emoji.Emojis
import net.greemdev.cabinet.lib.kordex.get
import net.greemdev.cabinet.botConfig
import net.greemdev.cabinet.database.entities.Question
import net.greemdev.cabinet.database.entities.VoteType
import net.greemdev.cabinet.lib.kordex.CabinetExtension
import net.greemdev.cabinet.lib.kordex.createCheck
import net.greemdev.cabinet.lib.util.Quad
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction



const val VoteCommandName = "cabinet-vote"
const val StartQuestionCommandName = "start-question"

class QuestionsExtension : CabinetExtension("questions") {

    override suspend fun setup() {
        ephemeralSlashCommand(::StartQuestionModalForm) {
            name = StartQuestionCommandName
            description = "Pose a question to the Cabinet. This is not intended for regular users."

            createCheck {
                if (event.interaction.channel.id != botConfig.cabinetChannel)
                    fail("Can only post cabinet questions in the cabinet channel.")
                if (botConfig.locked)
                    fail("Bot is locked.")
            }
            action { modal ->
                val (rawQuestion, rawRationale, rawPros, rawCons) = modal?.let {
                    Quad(it.question.get(), it.rationale.get(), it.pros.get(), it.cons.get())
                } ?: run {
                    respond {
                        content = "Fill in the popup! Run the command again!"
                    }
                    return@action
                }

                val question = transaction {
                    Question.new {
                        asker = user.id
                        question = rawQuestion
                        rationale = rawRationale
                        pros = when(rawPros) {
                            "" -> listOf()
                            else -> rawPros.split(';')
                        }
                        cons = when(rawCons) {
                            "" -> listOf()
                            else -> rawCons.split(';')
                        }
                    }
                }

                respond {
                    content = Emojis.ballotBoxWithCheck.unicode
                }

                val questionMessage = channel.createEmbed { question.questionEmbed() }

                transaction {
                    question.questionMessage = questionMessage.id
                }
            }
        }

        ephemeralSlashCommand(CommandArgs::Vote) {
            name = VoteCommandName
            description = "Vote for a specific question."
            createCheck {
                if (botConfig.locked)
                    fail("Bot is locked.")
            }

            action {
                val type = VoteType.fromCommandArg(arguments.voteType)
                val question = transaction {
                    Question.findById(arguments.id.toLong())
                } ?: run {
                     respond {
                         content = "Couldn't find a question with that ID."
                     }
                    return@action
                }

                if (question.isImmutable) {
                    respond {
                        content = "You can no longer vote for that question as it has already been voted for by everybody."
                    }
                    return@action
                }

                val voteResult = transaction { question.handleVote(type, user.id) }

                when (val t = voteResult.exceptionOrNull()) {
                    null -> {
                        question.updatePostedMessage()

                        respond {
                            content = "You have voted ${type.phrase()}. You can change your vote at any time via using the same command again, as long as the question is still active!"
                        }
                    }
                    else -> {
                        respond {
                            content = t.message
                        }
                    }
                }
            }
        }
    }
}

class StartQuestionModalForm : ModalForm() {
    override var title = "Ask a question"

    val question = lineText {
        label = "What would you like to ask in the Cabinet?"
        placeholder = "infinite diamonds for everyone"
    }

    val rationale = paragraphText {
        label = "What's the rationale for this change?"
        placeholder = "why not"
    }

    val pros = paragraphText {
        label = "(Optional) List the positives."
        placeholder = "Separate your ideas with ;"
        required = false
    }

    val cons = paragraphText {
        label = "(Optional) List the negatives."
        placeholder = "Separate your ideas with ;"
        required = false
    }
}