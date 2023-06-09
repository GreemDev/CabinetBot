package net.greemdev.cabinet

import net.greemdev.cabinet.lib.Version
import net.greemdev.cabinet.lib.nixargs.Options
import net.greemdev.cabinet.lib.nixargs.cli
import net.greemdev.cabinet.lib.util.slf4j
import java.io.File

var nostart = false

val CLI = cli {
    commandHandler(::runCommands)
    options {
        option("version", "Prints the version to the console.")
        option("nostart", "Exits the program when the command-line arguments are finished.")
        option("resetconfig", "Reset the bot config file. Causes the 'unlock' and 'token' options to be ignored.")
        option("resetdb", "Reset the bot database file; requiring confirmation.")
        option("unlock", "Unlock the bot, allowing for votes to be held.")
        option("token", "Use the provided token to login, inserting it into the config overwriting whatever may be there.") {
            requiredArg {
                describedAs("The token")
            }
        }
    }
}

fun runCommands(opts: Options) {
    val logger by slf4j("CLI")
    when {
        "resetdb" in opts -> {
            logger.info { "To confirm deletion of the database, type 'yes' and hit enter." }
            if (readlnOrNull().equals("yes", true)) {
                if (File("data/cabinet.mv.db").delete())
                    logger.info { "Database file has been deleted." }
            } else
                logger.info { "Database deletion aborted." }
        }
        "version" in opts -> logger.info("CabinetBot V${Version.formatted()}")
        "resetconfig" in opts -> {
            BotConfig.write()
            logger.info("Config at ${BotConfig.file().absolutePath} has been reset to its defaults.")
        }
        "unlock" in opts && "resetconfig" !in opts -> {
            BotConfig.write(
                botConfig.copy(locked = false)
            )
            logger.info { "CabinetBot has been unlocked." }
        }
        "nostart" in opts -> nostart = true
        "token" in opts && "resetconfig" !in opts -> {
            BotConfig.write(
                botConfig.copy(
                    token = opts["token"].getAs<String>()
                )
            )
            logger.info { "CabinetBot token has been changed & updated." }
        }
    }
}