package net.greemdev.cabinet.lib.kordex

import com.kotlindiscord.kord.extensions.extensions.Extension
import net.greemdev.cabinet.lib.util.invoking
import net.greemdev.cabinet.lib.util.slf4j

abstract class CabinetExtension(name: String) : Extension() {

    final override val name: String

    init {
        this.name = name.lowercase()
    }

    open val logger by slf4j { this::class.simpleName!!.dropLast("extension".length) }
    val cabinet by invoking { bot as CabinetBot }
}