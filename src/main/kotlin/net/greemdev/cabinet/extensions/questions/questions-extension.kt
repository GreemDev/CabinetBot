package net.greemdev.cabinet.extensions.questions

import com.kotlindiscord.kord.extensions.commands.application.slash.ephemeralSubCommand
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import net.greemdev.cabinet.botConfig
import net.greemdev.cabinet.database.entities.Question
import net.greemdev.cabinet.lib.kordex.CabinetExtension
import net.greemdev.cabinet.extensions.arg.*
import net.greemdev.cabinet.extensions.modal.*
import net.greemdev.cabinet.lib.kordex.cacheNonnullThenGet
import net.greemdev.cabinet.lib.kordex.createCheck
import org.jetbrains.exposed.sql.transactions.transaction

const val CabinetCommandName = "cabinet"
const val VoteSubcommandName = "vote"
const val StartQuestionSubcommandName = "start-question"
const val TiebreakerSubcommandName = "tiebreaker"
const val VoteCommandName = "$CabinetCommandName $VoteSubcommandName"
const val StartQuestionCommandName = "$CabinetCommandName $StartQuestionSubcommandName"
const val TiebreakerCommandName = "$CabinetCommandName $TiebreakerSubcommandName"

class QuestionsExtension : CabinetExtension("questions") {
    override suspend fun setup() {
        ephemeralSlashCommand {
            name = CabinetCommandName

            ephemeralSubCommand(::StartQuestionForm) {
                name = StartQuestionSubcommandName
                description = "Pose a question to the Cabinet. This is not intended for regular users."

                createCheck {
                    if (event.interaction.channel.id != botConfig.cabinetChannel)
                        fail("Can only post cabinet questions in the cabinet channel.")
                    if (botConfig.locked)
                        fail("Bot is locked.")
                }
                action(startQuestion)
            }

            ephemeralSubCommand(::Vote) {
                name = VoteSubcommandName
                description = "Vote for a specific question."
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

                action(vote)
            }
        }
    }
}