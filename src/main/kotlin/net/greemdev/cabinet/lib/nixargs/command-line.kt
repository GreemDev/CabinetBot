package net.greemdev.cabinet.lib.nixargs

import joptsimple.OptionParser
import net.greemdev.cabinet.lib.util.*

fun cli(builder: CliBuilder.() -> Unit) = CliBuilder().apply(builder).build()

class CliBuilder {

    private fun validate() {
        require(::optionParser.hasValue()) {
            "No available options."
        }
    }

    private lateinit var optionParser: OptionParser
    fun options(shortSpec: String? = null, builder: OptionParserBuilder.() -> Unit) {
        optionParser = buildOptionParser(shortSpec, builder)
    }

    private var commandHandler: (suspend Options.() -> Unit)? = null
    fun commandHandler(handler: suspend Options.() -> Unit) {
        commandHandler = handler
    }

    internal fun build(): CommandLine {
        validate()
        return CommandLine(optionParser, commandHandler)
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