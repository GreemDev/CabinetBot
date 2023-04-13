package net.greemdev.cabinet.lib.util

import kotlinx.datetime.Instant
import net.greemdev.cabinet.lib.util.parse.ParsingError
import java.nio.ByteBuffer
import java.util.*

fun CharSequence.charAfter(index: Int) = this[index.inc()]
fun CharSequence.charBefore(index: Int) = this[index.dec()]

fun CharSequence.splitByNewLine() = split('\n')
fun CharSequence.splitBySingleSpace() = split(' ')

inline fun <TIn, TOut> Parser(crossinline parseBlock: (TIn) -> TOut) =
    object : Parser<TIn, TOut>() {
        override fun parse(value: TIn): TOut = parseBlock(value)
    }

abstract class Parser<in TInput, TOutput> {
    protected abstract fun parse(value: TInput): TOutput

    @Throws(ParsingError::class)
    infix fun unsafeParse(value: TInput): TOutput = parse(value)
    operator fun invoke(value: TInput) = unsafeParse(value)
    infix fun tryParse(value: TInput): Result<TOutput> = runCatching { parse(value) }
}

data class StringScope(val inner: StringBuilder) : CharSequence by inner, Comparable<StringBuilder> by inner,
    java.io.Serializable by inner {

    inline operator fun <reified T> T?.unaryPlus() {
        inner.append(string())
    }

    operator fun Array<Any?>.unaryPlus() {
        forEach { inner.append(it.string()) }
    }

    operator fun Collection<Any?>.unaryPlus() {
        forEach { inner.append(it.string()) }
    }

    /**
     * Appends new line to the current [Any]? value's string representation.
     */
    fun Any?.newline() = "${string()}\n"
    fun newline() = '\n'

    /**
     * Prepends new line to the string representation of the provided [value].
     */
    fun line(value: Any?) = "\n${value.string()}"

    fun append(content: Any?): StringScope {
        inner.append(content.string())
        return this
    }

    fun appendln(content: Any? = "%JUSTNEWLINE%"): StringScope {
        if (content == "%JUSTNEWLINE%") {
            inner.appendLine()
        } else {
            inner.appendLine(content.string())
        }
        return this
    }

    fun currentString() = inner.toString()
}

inline fun string(initial: CharSequence = "", builderScope: StringScope.() -> Unit): String {
    return StringScope(StringBuilder(initial)).apply(builderScope).currentString()
}

inline fun buildString(initialValue: String, builderAction: StringBuilder.() -> Unit): String {
    return StringBuilder(initialValue).apply(builderAction).toString()
}


fun String.trimBefore(index: Int) = substring(index, (length + 1).coerceAtMost(length))
fun String.trimAfter(index: Int) = substring(0, index + 1)


operator fun CharSequence.times(factor: Int): String = repeat(factor)
operator fun Char.times(factor: Int): String = "$this" * factor

fun Instant.prettyPrint(): String {
    val instStrArr = string().split("T")
    val date = instStrArr.first().split("-")
    val time = instStrArr[1].split(".").first()

    return "${date[2]}/${date[1]}/${date[0]}, $time"
}

fun StringBuilder.appendIf(condition: Boolean, func: () -> String) {
    if (condition) append(func())
}

fun String.pluralize(quantity: Number, useES: Boolean = false, prefixQuantity: Boolean = true) =
    if (quantity != 1) string {
        if (prefixQuantity) +"$quantity "
        +this@pluralize
        if (useES) +'e'
        +'s'
    } else {
        if (prefixQuantity) "$quantity $this"
        else this
    }


inline fun String.fromEnd(func: String.() -> String): String = reversed().func().reversed()

fun String.isNumeric(): Boolean = isNotEmpty() and trim().all(Char::isDigit)

fun String.parseUUID(): UUID? {
    return tryOrNull {
        UUID.fromString(this)
    } ?: tryOrNull {
        val newBuffer = ByteBuffer.wrap(Base64.getUrlDecoder().decode("$this=="))
        UUID(newBuffer.long, newBuffer.long)
    }
}

fun UUID.shorten(): String {
    val buffer = ByteBuffer.wrap(ByteArray(16)).apply {
        putLong(mostSignificantBits)
        putLong(leastSignificantBits)
    }

    return Base64.getUrlEncoder().encodeToString(buffer.array()).trimEnd { it == '=' }
}

/**
 * Extension version of [stringOf].
 * @sample stringOf
 */
fun Any?.string() = stringOf(this)

/**
 * If the [value] is null, return the literal string "null", otherwise return human-readable string of [value].
 */
fun stringOf(value: Any?): String = with(value) {
    when (this) {
        is StringScope -> currentString()
        null -> "null"
        else -> toString()
    }
}
