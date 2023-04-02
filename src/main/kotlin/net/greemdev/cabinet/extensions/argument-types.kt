package net.greemdev.cabinet.extensions

import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.converters.impl.stringChoice
import com.kotlindiscord.kord.extensions.commands.converters.impl.*

object CommandArgs {
    class Vote : Arguments() {
        val id by int {
            name = "vote-id"
            description = "The numerical non-decimal ID of the question you'd like to vote on."
        }

        val voteType by stringChoice {
            name = "type"
            description = "What is your vote?"
            choices(
                mapOf(
                    "I support this" to "1",
                    "I am against this" to "0",
                    "I am choosing to abstain" to "-1"
                )
            )
        }
    }

    open class ProsCons : Arguments() {
        val id by int {
            name = "vote-id"
            description = "The numerical non-decimal ID of the question."
        }

        class AddPro : ProsCons() {
            val pro by string {
                name = "pro"
                description = "A positive of this proposed change."
            }
        }

        class AddCon : ProsCons() {
            val con by string {
                name = "con"
                description = "A downside to this proposed change."
            }
        }
    }
}