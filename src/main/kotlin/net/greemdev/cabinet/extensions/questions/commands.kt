package net.greemdev.cabinet.extensions.questions

import com.kotlindiscord.kord.extensions.types.respond
import dev.kord.common.kColor
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.channel.createMessage
import dev.kord.rest.builder.message.create.embed
import dev.kord.x.emoji.Emojis
import kotlinx.coroutines.flow.toList
import net.greemdev.cabinet.database.entities.Question
import net.greemdev.cabinet.database.entities.VoteType
import net.greemdev.cabinet.extensions.arg.Vote
import net.greemdev.cabinet.extensions.*
import net.greemdev.cabinet.extensions.arg.QuestionArguments
import net.greemdev.cabinet.extensions.arg.QuestionTiebreaker
import net.greemdev.cabinet.extensions.modal.StartQuestionForm
import net.greemdev.cabinet.lib.kordex.*
import net.greemdev.cabinet.lib.util.fromQuestion
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import java.awt.Color

val startQuestion by ephemeralCommandRequireModalOnly<StartQuestionForm> { modal ->

    val bot by CabinetBot

    val question = transaction {
        Question.new {
            asker = user.id
            question = modal.question.get()
            rationale = modal.rationale.get()
            pros = modal.pros.getListValue()
            cons = modal.cons.getListValue()
        }
    }

    respond {
        content = Emojis.ballotBoxWithCheck.unicode
    }

    val questionMessage = channel.createEmbed { fromQuestion(question) }

    val cabinetMembers = bot.getCabinetMembers().toList()

    val added = transaction {
        question.questionMessage = questionMessage.id

        question.addAutoAbstainers(cabinetMembers)
    }
    if (added > 0)
        question.updatePostedMessage()
}

val vote by ephemeralCommand<Vote> {
    val type = VoteType.fromCommandArg(arguments.voteType)
    val question: Question by cache

    newSuspendedTransaction { question.handleVote(type, user.id) }
        .onSuccess {
            respond {
                content = "You have voted ${type.phrase}. You can change your vote at any time via using the same command again, as long as the question is still active!"
            }

            question.updatePostedMessage()
        }.onFailure {
            respond {
                content = it.message
            }
        }
}

val tiebreaker by ephemeralCommand<QuestionTiebreaker> {
    val question: Question by cache

    if (question.presidentialOverride != null) {
        respond {
            content = "Cannot break a tie on a question that has already been through the process."
        }
        return@ephemeralCommand
    }

    if (arguments.tiebreakerOverride) {
        respond {
            content = "The tie has been broken."
        }
        channel.createMessage {
            messageReference = question.questionMessage
            embed {
                color = Color.GREEN.brighter().kColor
                title = "${question.id.value} - ${question.question} | Tiebreaker"
                description = "This question was forcibly overridden by the current president, thus breaking the tie, and approving the question or proposal."
            }
        }
    } else {
        channel.createMessage {
            messageReference = question.questionMessage
            embed {
                color = Color.RED.darker().kColor
                title = "${question.id.value} - ${question.question} | Tiebreaker"
                description = "The current President has chosen not to break the tie and keep the deadlock."
            }
        }
    }
    transaction { question.presidentialOverride = arguments.tiebreakerOverride }
}

val query by ephemeralCommand<QuestionArguments> {

}
