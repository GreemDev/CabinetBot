package net.greemdev.cabinet.extensions.questions

import com.kotlindiscord.kord.extensions.types.respond
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.x.emoji.Emojis
import net.greemdev.cabinet.database.entities.Question
import net.greemdev.cabinet.database.entities.VoteType
import net.greemdev.cabinet.extensions.arg.Vote
import net.greemdev.cabinet.extensions.ephemeralCommand
import net.greemdev.cabinet.extensions.ephemeralCommandRequireModal
import net.greemdev.cabinet.extensions.modal.StartQuestionForm
import net.greemdev.cabinet.lib.kordex.get
import net.greemdev.cabinet.lib.kordex.getListValue
import net.greemdev.cabinet.lib.kordex.getValue
import net.greemdev.cabinet.lib.util.fromQuestion
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

val startQuestion by ephemeralCommandRequireModal<StartQuestionForm> { modal ->

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

    transaction {
        question.questionMessage = questionMessage.id

        question.addAutoAbstainers()
    }
}

val vote by ephemeralCommand<Vote> {
    val type = VoteType.fromCommandArg(arguments.voteType)
    val question: Question by cache

    val exception = newSuspendedTransaction { question.handleVote(type, user.id) }

    if (exception == null) {
        respond {
            content = "You have voted ${type.phrase()}. You can change your vote at any time via using the same command again, as long as the question is still active!"
        }

        if (question.getMissingVoters().isEmpty())
            transaction { question.isImmutable = true }

        question.updatePostedMessage()
    } else {
        respond {
            content = exception.message
        }
    }
}
