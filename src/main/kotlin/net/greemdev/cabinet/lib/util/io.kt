package net.greemdev.cabinet.lib.util

import java.io.File

fun modifyFileContent(path: String, block: StringScope.() -> Unit) {
    with(File(path)) {
        if (!exists())
            mkdirs()
        writeText(string(tryOrNull { readText() } ?: "", block))
    }
}