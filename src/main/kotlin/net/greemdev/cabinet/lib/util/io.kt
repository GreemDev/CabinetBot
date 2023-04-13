package net.greemdev.cabinet.lib.util

import java.io.File

fun modifyFileContent(path: String, block: StringScope.() -> Unit) {
    with(File(path)) {
        if (!exists())
            mkdirs()
        writeText(string(tryOrNull { readText() } ?: "", block))
    }
}

var File.isReadable: Boolean
    get() = canRead()
    set(value) { setReadable(value) }

var File.isWritable: Boolean
    get() = canWrite()
    set(value) { setWritable(value) }

var File.isExecutable: Boolean
    get() = canExecute()
    set(value) { setExecutable(value) }