package net.greemdev.cabinet.lib.util.parse

class ParsingError(override val message: String?, override val cause: Throwable? = null) : Throwable(message, cause) {
    companion object {
        fun throwing(message: String, cause: Throwable? = null, block: ExceptionScope.() -> Throwable): Nothing {
            throw ExceptionScope(message, cause).block()
        }
    }

    class ExceptionScope(val message: String, val cause: Throwable? = null) {
        fun badInput() = ParsingError("Bad input: $message", cause)
        fun format() = ParsingError("Invalid format: $message", cause)
        fun emptyParse() = ParsingError("No recognizable $message found in the input value", cause)
    }
}