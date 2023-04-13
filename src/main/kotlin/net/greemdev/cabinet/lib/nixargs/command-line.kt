package net.greemdev.cabinet.lib.nixargs

import joptsimple.OptionParser
import net.greemdev.cabinet.lib.util.*
import java.util.Optional
import kotlin.jvm.optionals.getOrNull

fun cli(builder: CliBuilder.() -> Unit) = CliBuilder().apply(builder).build()

class CliBuilder {

    private var optionParser: Optional<OptionParser> = optionalOf()
    private var commandHandler: Optional<suspend Options.() -> Unit> = optionalOf()

    fun options(shortSpec: String? = null, builder: OptionParserBuilder.() -> Unit) {
        optionParser = optionalOf(optionParser(shortSpec, builder))
    }

    fun commandHandler(handler: suspend Options.() -> Unit) {
        commandHandler = optionalOf(handler)

    }

    internal fun build(): CommandLine {
        validate()
        return CommandLine(optionParser.get(), commandHandler.getOrNull())
    }

    private fun validate() {
        require(optionParser.isPresent) {
            "No available options."
        }
    }
}

class CommandLine(
    private val parser: OptionParser,
    private val commandHandler: (suspend Options.() -> Unit)?
) {
    infix fun parseInput(input: Array<out String>) = Options(parser.parse(*input))
    infix fun parseInput(input: Collection<String>) = parseInput(input.array())
    infix fun parseInput(input: String) = parseInput(input.split(" "))

    suspend infix fun handleCommands(input: Array<out String>) = parseInput(input).also {
        commandHandler?.invoke(it)
    }
}