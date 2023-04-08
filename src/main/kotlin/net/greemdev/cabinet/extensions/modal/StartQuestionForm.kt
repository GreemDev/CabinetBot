package net.greemdev.cabinet.extensions.modal

import com.kotlindiscord.kord.extensions.components.forms.ModalForm

class StartQuestionForm : ModalForm() {
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