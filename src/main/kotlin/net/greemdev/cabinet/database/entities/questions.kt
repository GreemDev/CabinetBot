package net.greemdev.cabinet.database.entities

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.edit
import dev.kord.core.behavior.getChannelOfOrNull
import dev.kord.core.entity.channel.TextChannel
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.builder.message.modify.embed
import kotlinx.serialization.Serializable
import net.greemdev.cabinet.botConfig
import org.jetbrains.exposed.dao.*
import org.jetbrains.exposed.dao.id.*
import net.greemdev.cabinet.database.entities.json.*
import net.greemdev.cabinet.database.x.*
import net.greemdev.cabinet.extensions.*
import net.greemdev.cabinet.lib.kordex.koinInject
import net.greemdev.cabinet.lib.util.*

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
    companion object : EntityClass<Long, Question>(Questions)

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

    fun handleVote(type: VoteType, id: Snowflake) = runCatching {
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
    }


    var isImmutable by Questions.hasEnded

    var pros: Collection<String> by serializedJson(Questions.pros)

    fun modifyPros(accumulator: AccumulatorFunc<String>) {
        pros = pros accumulate accumulator
    }

    val formattedPros by invoking {
        if (pros.isEmpty())
            "None yet, add one via /$AddProCommandName"
        else
            pros.joinToString("\n") { " - $it" }
    }

    var cons: Collection<String> by serializedJson(Questions.cons)

    fun modifyCons(accumulator: AccumulatorFunc<String>) {
        pros = pros accumulate accumulator
    }

    val formattedCons by invoking {
        if (cons.isEmpty())
            "None yet, add one via /$AddConCommandName"
        else
            cons.joinToString("\n") { " - $it" }
    }

    val formattedVotes by invoking {

        val votesInFavor = positivesVotes.voters.size
        val votesAgainst = negativeVotes.voters.size
        val votesAbstained = abstainedVotes.voters.size

        "$votesInFavor in favor, $votesAgainst against, with $votesAbstained abstaining || Vote via /$VoteCommandName"
    }

    context(EmbedBuilder)
    fun questionEmbed() {
        title = "${id.value} - $question"
        field {
            name = "Why?"
            value = rationale
        }
        field {
            inline = true
            name = "Pros"
            value = formattedPros
        }
        field {
            inline = true
            name = "Cons"
            value = formattedCons
        }
        footer {
            text = formattedVotes
        }
    }

    suspend fun updatePostedMessage(): Boolean {
        val kord by koinInject<Kord>()

        val channel = kord.getGuildOrNull(858547359804555264.snowflake)
                ?.getChannelOfOrNull<TextChannel>(botConfig.cabinetChannel)
                ?: return false

        val message = channel.getMessageOrNull(questionMessage) ?: return false

        message.edit {
            embed { questionEmbed() }
        }
        return true
    }
}

@Serializable
data class VoteData(
        val voters: Collection<Snowflake> = listOf()
) {
    fun copyEditVoters(accumulator: AccumulatorFunc<Snowflake>) = copy(
            voters = voters accumulate accumulator
    )
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