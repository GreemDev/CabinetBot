package net.greemdev.cabinet.database.entities

import com.kotlindiscord.kord.extensions.utils.getJumpUrl
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.edit
import dev.kord.core.entity.Member
import dev.kord.core.entity.Message
import dev.kord.rest.builder.message.create.embed
import dev.kord.rest.builder.message.modify.embed
import kotlinx.coroutines.flow.toList
import net.greemdev.cabinet.botConfig
import org.jetbrains.exposed.dao.*
import org.jetbrains.exposed.dao.id.*
import net.greemdev.cabinet.database.x.*
import net.greemdev.cabinet.extensions.procon.AddConCommandName
import net.greemdev.cabinet.extensions.procon.AddProCommandName
import net.greemdev.cabinet.extensions.questions.TiebreakerCommandName
import net.greemdev.cabinet.extensions.questions.VoteCommandName
import net.greemdev.cabinet.lib.kordex.CabinetBot
import net.greemdev.cabinet.lib.kordex.koinComponent
import net.greemdev.cabinet.lib.util.*
import org.jetbrains.exposed.sql.Transaction
import kotlin.jvm.Throws

object Questions : LongIdTable() {
    val asker = ulong("askerId")
    val question = varchar("question", 1000)
    val rationale = varchar("reasoning", 1500)
    val postedMessage = ulong("messageId").default(0u)
    val hasEnded = bool("ended").default(false)
    val positiveVotes = json("pos", listOf<Snowflake>())
    val negativeVotes = json("neg", listOf<Snowflake>())
    val abstainedVotes = json("abs", listOf<Snowflake>())
    val pros = json("positives", arrayOf<String>())
    val cons = json("negatives", arrayOf<String>())
    val wasOverriden = bool("overridden").nullable().default(null)
}

const val cantDoubleVote = "Can't double vote."

class Question(id: EntityID<Long>) : Entity<Long>(id) {
    companion object : EntityClass<Long, Question>(Questions) {
        context(Transaction)
        fun findConcludedVotes() =
            find { Questions.hasEnded eq true }.toList()
    }

    var asker by serializedSnowflake(Questions.asker)
    var question by Questions.question
    var rationale by Questions.rationale
    var presidentialOverride by Questions.wasOverriden
    var questionMessage by serializedSnowflake(Questions.postedMessage)

    var positivesVotes: Collection<Snowflake> by serializedJson(Questions.positiveVotes)

    fun modifyPositiveVotes(accumulator: AccumulatorFunc<Snowflake>) {
        positivesVotes = positivesVotes accumulate accumulator
    }

    var negativeVotes: Collection<Snowflake> by serializedJson(Questions.negativeVotes)

    fun modifyNegativeVotes(accumulator: AccumulatorFunc<Snowflake>) {
        negativeVotes = negativeVotes accumulate accumulator
    }

    var abstainedVotes: Collection<Snowflake> by serializedJson(Questions.abstainedVotes)

    fun modifyAbstainedVotes(accumulator: AccumulatorFunc<Snowflake>) {
        abstainedVotes = abstainedVotes accumulate accumulator
    }

    private fun addVoter(type: VoteType, id: Snowflake) {
        when (type) {
            VoteType.Yes -> modifyPositiveVotes { +id }
            VoteType.No -> modifyNegativeVotes { +id }
            VoteType.None -> modifyAbstainedVotes { +id }
        }
    }

    fun addAutoAbstainers(cabinetMembers: List<Member>) =
            botConfig.autoAbstain.let { abstainers ->
                modifyAbstainedVotes {
                    +abstainers.filter { abstainer ->
                        cabinetMembers.any { it.id == abstainer }
                    }
                }
                abstainers.size
            }


    context(Transaction)
    suspend fun handleVote(type: VoteType, id: Snowflake) = runCatching {
        if ((positivesVotes.contains(id) && type.inFavor())
            or (negativeVotes.contains(id) && type.against())
            or (abstainedVotes.contains(id) && type.abstain()))
            error(cantDoubleVote)

        if (positivesVotes.contains(id) && !type.inFavor())
            modifyPositiveVotes { -id }

        if (negativeVotes.contains(id) && !type.against())
            modifyNegativeVotes { -id }

        if (abstainedVotes.contains(id) && !type.abstain())
            modifyAbstainedVotes { -id }

        addVoter(type, id)
        if (getMissingVoters().isEmpty())
            isImmutable = true
    }


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
        val votesInFavor = positivesVotes.size
        val votesAgainst = negativeVotes.size
        val votesAbstained = abstainedVotes.size

        "$votesInFavor in favor, $votesAgainst against, with $votesAbstained abstaining || Vote via /$VoteCommandName"
    }

    @Throws(VoteTiedException::class)
    fun voteWinner(): VoteType? = if (isImmutable) {
        val positive = positivesVotes.size
        val negative = negativeVotes.size

        if (positive == negative) {
            throw VoteTiedException()
        } else {
            if (positive > negative)
                VoteType.Yes
            else VoteType.No
        }

    } else null

    suspend fun getMissingVoters(): List<Member> {
        return with(koinComponent<CabinetBot>().getCabinetMembers().toList().toMutableList()) {
            val allVoters = (positivesVotes + negativeVotes + abstainedVotes).toSet()
            removeAll { it.id in allVoters }
            this
        }
    }

    private fun embedMessageUrl(content: String, message: Message) = markdown(content).maskedUrl(message.getJumpUrl())

    private fun winnerString(winner: VoteType, voteMessage: Message) = string {
        if (winner.inFavor())
            +"${embedMessageUrl("This", voteMessage)} has passed, with ${positivesVotes.size - negativeVotes.size} more in favor than against, "
        else
            +"${embedMessageUrl("This", voteMessage)} **has not** passed, with ${negativeVotes.size - positivesVotes.size} more against than in favor, "

        +"and ${abstainedVotes.size} abstaining."
    }


    suspend fun updatePostedMessage() {
        val bot by CabinetBot

        val channel = bot.getCabinetChannel()

        val message = channel.getMessageOrNull(questionMessage) ?: return

        if (isImmutable) {
            try {
                val winner = voteWinner()!!
                channel.createMessage {
                    messageReference = message.id
                    content = bot.getCabinetRole().mention
                    embed {
                        title = "${id.value} - $question"
                        description = winnerString(winner, message)
                    }
                }
            } catch (_: VoteTiedException) {
                channel.createMessage {
                    messageReference = message.id
                    content = bot.getCurrentPresident().mention
                    embed {
                        title = "An ultra-rare tie has appeared!"
                        description = "There is a tie. Please decide if you'd like to make ${embedMessageUrl("this question", message)} pass, or to leave it as a deadlock, thus effectively vetoing the question, via /$TiebreakerCommandName"
                    }
                }
            }
        }
        message.edit {
            embed { fromQuestion(this@Question) }
        }
    }
}

class VoteTiedException : Exception()

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

    val phrase by invoking {
        when (this) {
            Yes -> "in favor"
            No -> "against"
            None -> "to abstain"
        }
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