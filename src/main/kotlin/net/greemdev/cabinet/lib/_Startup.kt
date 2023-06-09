package net.greemdev.cabinet.lib

import net.greemdev.cabinet.lib.util.*
import net.greemdev.cabinet.logger

const val separatorLine =
    "------------------------------------------------------------------------------------------"

private val asciiHeader = """
 _______  _______  ______  _________ _        _______ _________ ______   _______ _________
(  ____ \(  ___  )(  ___ \ \__   __/( (    /|(  ____ \\__   __/(  ___ \ (  ___  )\__   __/
| (    \/| (   ) || (   ) )   ) (   |  \  ( || (    \/   ) (   | (   ) )| (   ) |   ) (   
| |      | (___) || (__/ /    | |   |   \ | || (__       | |   | (__/ / | |   | |   | |   
| |      |  ___  ||  __ (     | |   | (\ \) ||  __)      | |   |  __ (  | |   | |   | |   
| |      | (   ) || (  \ \    | |   | | \   || (         | |   | (  \ \ | |   | |   | |   
| (____/\| )   ( || )___) )___) (___| )  \  || (____/\   | |   | )___) )| (___) |   | |   
(_______/|/     \||/ \___/ \_______/|/    )_)(_______/   )_(   |/ \___/ (_______)   )_(     ${Version.kotlin.formatted()}
""".trimIndent()

fun indentedHeaderLines() = asciiHeader.splitByNewLine().map { ' ' * 10 + it }


fun onStartup(sw: Stopwatch) {
    logger.info(separatorLine)
    indentedHeaderLines().forEach(logger::info)
    logger.info(separatorLine)
    logger.info { "CabinetBot v${Version.formatted()} is online, took ${sw.stop().ms()}." }
}