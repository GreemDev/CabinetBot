package net.greemdev.cabinet.database.entities

import com.kotlindiscord.kord.extensions.utils.getJumpUrl
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.edit
import dev.kord.core.behavior.getChannelOfOrNull
import dev.kord.core.entity.Member
import dev.kord.core.entity.channel.TextChannel
import dev.kord.rest.builder.message.create.embed
import dev.kord.rest.builder.message.modify.embed
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.Serializable
import net.greemdev.cabinet.botConfig
import org.jetbrains.exposed.dao.*
import org.jetbrains.exposed.dao.id.*
import net.greemdev.cabinet.database.x.*
import net.greemdev.cabinet.extensions.procon.AddConCommandName
import net.greemdev.cabinet.extensions.procon.AddProCommandName
import net.greemdev.cabinet.extensions.questions.VoteCommandName
import net.greemdev.cabinet.lib.kordex.CabinetBot
import net.greemdev.cabinet.lib.kordex.koinComponent
import net.greemdev.cabinet.lib.kordex.koinInject
import net.greemdev.cabinet.lib.util.*
import org.jetbrains.exposed.sql.Transaction
import kotlin.jvm.Throws

object Questions : LongIdTable() {
    val asker = ulong("askerId")
    val question = varchar("question", 1000)
    val rationale = varchar("reasoning", 1500)
    val postedMessage = ulong("messageId").default(0u)
    val hasEnded = bool("ended").default(false)
    val positiveVotes = json("pos", VoteData())
    val negativeVotes = json("neg", VoteData())
    val abstainedVotes = json("abs", VoteData())
    val pros = json("positives", arrayOf<String>())
    val cons = json("negatives", arrayOf<String>())
}

const val cantDoubleVote = "Can't double vote."

class Question(id: EntityID<Long>) : Entity<Long>(id) {
    companion object : EntityClass<Long, Question>(Questions) {
        context(Transaction)
        fun findConcludedVotes(): List<Question> {
            return Question.find { Questions.hasEnded eq true }.toList()
        }
    }

    var asker by serializedSnowflake(Questions.asker)
    var question by Questions.question
    var rationale by Questions.rationale
    var questionMessage by serializedSnowflake(Questions.postedMessage)

    var positivesVotes: VoteData by serializedJson(Questions.positiveVotes)
    var negativeVotes: VoteData by serializedJson(Questions.negativeVotes)
    var abstainedVotes: VoteData by serializedJson(Questions.abstainedVotes)

    private fun addVoter(type: VoteType, id: Snowflake) {
        when (type) {
            VoteType.Yes -> positivesVotes = positivesVotes.copyEditVoters { +id }
            VoteType.No -> negativeVotes = negativeVotes.copyEditVoters { +id }
            VoteType.None -> abstainedVotes = abstainedVotes.copyEditVoters { +id }
        }
    }

    fun addAutoAbstainers() =
            botConfig.autoAbstain.let {
                abstainedVotes = abstainedVotes.copyEditVoters {
                    +it
                }
                it.size
            }


    context(Transaction)
    suspend fun handleVote(type: VoteType, id: Snowflake) = runCatching {
        if (positivesVotes.voters.contains(id) && type.inFavor())
            error(cantDoubleVote)
        if (negativeVotes.voters.contains(id) && type.against())
            error(cantDoubleVote)
        if (abstainedVotes.voters.contains(id) && type.abstain())
            error(cantDoubleVote)

        if (positivesVotes.voters.contains(id) && !type.inFavor())
            positivesVotes = positivesVotes.copyEditVoters {
                -id
            }

        if (negativeVotes.voters.contains(id) && !type.against())
            negativeVotes = negativeVotes.copyEditVoters {
                -id
            }

        if (abstainedVotes.voters.contains(id) && !type.abstain())
            abstainedVotes = abstainedVotes.copyEditVoters {
                -id
            }

        addVoter(type, id)
        if (getMissingVoters().isEmpty()) {
            isImmutable = true
        }
    }.exceptionOrNull()


    var isImmutable by Questions.hasEnded

    var pros: Collection<String> by serializedJson(Questions.pros)

    context(Transaction)
    fun modifyPros(accumulator: AccumulatorFunc<String>) {
        pros = pros accumulate accumulator
    }

    val formattedPros by invoking {
        if (pros.isEmpty())
            "None yet, add one via /$AddProCommandName"
        else
            markdown(pros.joinToString("\n") { "+ $it" }).blockCode("diff")
    }

    var cons: Collection<String> by serializedJson(Questions.cons)

    context(Transaction)
    fun modifyCons(accumulator: AccumulatorFunc<String>) {
        cons = cons accumulate accumulator
    }

    val formattedCons by invoking {
        if (cons.isEmpty())
            "None yet, add one via /$AddConCommandName"
        else
            markdown(cons.joinToString("\n") { "- $it" }).blockCode("diff")
    }

    val formattedVotes by invoking {
        val votesInFavor = positivesVotes.voters.size
        val votesAgainst = negativeVotes.voters.size
        val votesAbstained = abstainedVotes.voters.size

        "$votesInFavor in favor, $votesAgainst against, with $votesAbstained abstaining || Vote via /$VoteCommandName"
    }

    @Throws(VoteTiedException::class)
    suspend fun voteWinner(): VoteType? = if (isImmutable) {
        val positive = positivesVotes.voters.size
        val negative = negativeVotes.voters.size

        if (positive == negative) {
            throw VoteTiedException()
        } else {
            if (positive > negative)
                VoteType.Yes
            else VoteType.No
        }

    } else null

    suspend fun getMissingVoters(): Collection<Member> =
            koinComponent<CabinetBot>().getCabinetMembers().filter {
                it.id !in positivesVotes.voters || it.id !in negativeVotes.voters || it.id !in abstainedVotes.voters
            }.toList()


    suspend fun updatePostedMessage(): Boolean {
        val bot by koinInject<CabinetBot>()

        val channel = bot.getPrismGuild()
                .getChannelOfOrNull<TextChannel>(botConfig.cabinetChannel)
                ?: return false

        val message = channel.getMessageOrNull(questionMessage)

        return if (isImmutable) {
            if (message == null)
                false
            else try {
                val winner = voteWinner()!!
                channel.createMessage {
                    content = bot.getCabinetMemberRole().mention
                    embed {
                        title = "${id.value} - $question"
                        description = if (winner.inFavor())
                            "This has passed, with ${positivesVotes.voters.size - negativeVotes.voters.size} more in favor than against, and ${abstainedVotes.voters.size} abstaining."
                        else
                            "This **has not** passed, with ${negativeVotes.voters.size - positivesVotes.voters.size} more against than in favor, and ${abstainedVotes.voters.size} abstaining."
                    }
                }
                message.delete()
                false
            } catch (e: VoteTiedException) {
                channel.createMessage {
                    content = bot.getCurrentPresident().mention
                    embed {
                        title = "An ultra-rare tie has appeared!"
                        description = e.message!!.format(message.getJumpUrl())
                    }
                }
                false
            }
        } else {
            if (message == null)
                false
            else {
                message.edit {
                    embed { fromQuestion(this@Question) }
                }
                true
            }
        }
    }
}

class VoteTiedException : Exception("There is a tie. Please independently decide if you'd like to make [this question](%s) pass, or to leave it as a deadlock and thus veto it, via /cabinet-question-override")

@Serializable
data class VoteData(
        val voters: Collection<Snowflake> = listOf()
) {
    fun copyEditVoters(accumulator: AccumulatorFunc<Snowflake>) =
            copy(voters = voters accumulate accumulator)
}

enum class VoteType {
    Yes, No, None;

    fun inFavor() = this == Yes
    fun against() = this == No
    fun abstain() = this == None

    override fun toString() = when (this) {
        Yes -> "Yay"
        No -> "Nay"
        None -> "Abstain"
    }

    fun phrase() = when (this) {
        Yes -> "in favor"
        No -> "against"
        None -> "to abstain"
    }

    companion object {
        fun fromCommandArg(number: String) =
                when (number) {
                    "1" -> Yes
                    "0" -> No
                    "-1" -> None
                    else -> error("nah")
                }
    }
}