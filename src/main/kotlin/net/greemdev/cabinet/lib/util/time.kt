package net.greemdev.cabinet.lib.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.toKotlinInstant
import java.util.*
import kotlin.time.Duration

class Stopwatch {
    companion object {
        fun startNew() = Stopwatch().apply(Stopwatch::start)
        fun measureExecutionTime(block: () -> Unit) = with(startNew()) {
            block()
            stop()
        }

        suspend fun CoroutineScope.measureExecutionTime(asyncBlock: suspend CoroutineScope.() -> Unit) =
            with(startNew()) {
                asyncBlock(this@measureExecutionTime)
                stop()
            }
    }

    var start: Optional<Instant> = optionalOf()
        private set
    var end: Optional<Instant> = optionalOf()
        private set

    fun start() {
        if (start.isEmpty) {
            start = optionalOf(Clock.System.now())
        }
    }

    fun stop(): Duration {
        check(start.isPresent) {
            "Cannot end a Stopwatch that has not started."
        }
        end = optionalOf(Clock.System.now())
        return elapsed()
    }

    fun elapsed(): Duration {
        check(start.isPresent and end.isPresent) {
            "Cannot calculate the elapsed duration without both a start and end point."
        }
        return compare(end.get() to start.get()) { timeSince() }
    }

}

fun Duration.ms(): String = "${inWholeMilliseconds}ms"

inline fun<T> compare(pair: Pair<Instant, Instant>, block: TimePairComparer.() -> T): T = object : TimePairComparer(pair) {}.block()


fun Instant.comparer() = object : TimeComparer(this) {}
fun java.time.Instant.comparer() = toKotlinInstant().comparer()

abstract class TimePairComparer(val pairing: Pair<Instant, Instant>) {

    fun timeSince(): Duration {
        val (left, right) = pairing
        return left.comparer().timeSince(right)
    }

    fun timeUntil(): Duration {
        val (left, right) = pairing
        return left.comparer().timeUntil(right)
    }

    fun isSame() = pairing.first == pairing.second
    fun isAfter() = pairing.first.comparer().succeeds(pairing.second)
    fun isBefore() = pairing.first.comparer().precedes(pairing.second)
}


@Suppress("NOTHING_TO_INLINE")
abstract class TimeComparer(val instant: Instant) {
    fun timeSince(pastInstant: Instant): Duration {
        check(succeeds(pastInstant)) {
            "provided instant in #timeSince parameter must be in the past!"
        }
        return instant - pastInstant
    }

    fun timeUntil(futureInstant: Instant): Duration {
        check(precedes(futureInstant)) {
            "provided instant in #timeUntil parameter must be in the future!"
        }
        return futureInstant - instant
    }

    inline fun currentOrBefore(other: Instant) = isCurrent(other) || precedes(other)
    inline fun currentOrAfter(other: Instant) = isCurrent(other) || succeeds(other)

    inline fun isCurrent(other: Instant) = other == instant

    /**
     * Check whether the current [Instant] chronologically comes before the provided [Instant] inst.
     */
    inline fun precedes(inst: Instant) = instant < inst

    /**
     * Check whether the current [Instant] instant chronologically comes after the provided [Instant] inst.
     */
    inline fun succeeds(inst: Instant) = instant > inst
}