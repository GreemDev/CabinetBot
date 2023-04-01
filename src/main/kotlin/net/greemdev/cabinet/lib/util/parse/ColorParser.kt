package net.greemdev.cabinet.lib.util.parse

import net.greemdev.cabinet.lib.util.Parser
import java.awt.Color

object ColorParser : Parser<String, Color>() {
    override fun parse(value: String): Color {
        if (value.length !in 6..7)
            ParsingError.throwing("must be a 6-character hexadecimal value with or without the preceding #") { format() }

        val rawHex = value.takeLast(6).uppercase()
            .takeIf { chars ->
                chars.all {
                    it in '0'..'9' || it in 'A'..'F'
                }
            } ?: ParsingError.throwing("hex characters must be within 0-9 or A-F.") { format() }

        // not sure if kord Color and awt Color behave the same with this value, need to test
        return Color(rawHex.toInt(16))
    }
}