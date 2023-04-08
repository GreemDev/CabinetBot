package net.greemdev.cabinet.extensions.procon

import com.kotlindiscord.kord.extensions.types.respond
import dev.kord.common.Color
import dev.kord.rest.builder.message.create.actionRow
import dev.kord.rest.builder.message.create.embed
import net.greemdev.cabinet.database.entities.Question
import net.greemdev.cabinet.extensions.arg.AddCon
import net.greemdev.cabinet.extensions.arg.AddPro
import net.greemdev.cabinet.extensions.arg.QuestionArguments
import net.greemdev.cabinet.extensions.ephemeralCommand
import net.greemdev.cabinet.lib.kordex.getValue
import net.greemdev.cabinet.lib.util.*

val addPro by ephemeralCommand<AddPro> {
    val question: Question by cache

    val charactersExceedingLimit = arguments.pro.length - 100
    if (charactersExceedingLimit > 0) {
        respond {
            embed {
                color = Color.embedDefault
                title = "That was too long."
                description =
                    "Please limit the input to 100 characters; your input was $charactersExceedingLimit too long. The input is the raw content of this message so you can copy it easily."
            }
            content = arguments.pro
        }
        return@ephemeralCommand
    }

    question.updatePostedMessage()

    respond {
        content = "Added the benefit ${markdown(arguments.pro).inlineCode()} to question with ID ${arguments.id}"
    }
}

val removePro by ephemeralCommand<QuestionArguments> {
    val question: Question by cache

    respond {
        embed {
            title = "Please select one or multiple options below to remove."
        }
        actionRow { prosSelect(question) }
    }
}

val addCon by ephemeralCommand<AddCon> {
    val question: Question by cache

    val charactersExceedingLimit = arguments.con.length - 100
    if (charactersExceedingLimit > 0) {
        respond {
            embed {
                color = Color.embedDefault
                title = "That was too long."
                description =
                    "Please limit the input to 100 characters; your input was $charactersExceedingLimit too long. The input is the raw content of this message so you can copy it easily."
            }
            content = arguments.con
        }
        return@ephemeralCommand
    }

    question.updatePostedMessage()

    respond {
        content = "Added the downside ${markdown(arguments.con).inlineCode()} to question with ID ${arguments.id}"
    }
}

val removeCon by ephemeralCommand<QuestionArguments> {
    val question: Question by cache

    respond {
        embed {
            title = "Please select one or multiple options below to remove."
        }
        actionRow { consSelect(question) }
    }
}