package net.greemdev.cabinet.database.entities.json

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.sql.*
import net.greemdev.cabinet.database.x.ParsedDataBackedProperty
import net.greemdev.cabinet.database.x.h2StringMaxLength
import net.greemdev.cabinet.database.x.serialized
import net.greemdev.cabinet.lib.util.applyIf
import net.greemdev.cabinet.lib.util.formatJsonString
import net.greemdev.cabinet.lib.util.parseJsonString

/**
 * A property delegate for a Kotlin object whose value is represented in the Exposed [column] as a compound JSON string value.
 * The getter parses the underlying data, and the setter sets the underlying data to its newer counterpart, as JSON.
 * @param column The varchar column whose value should be treated as a JSON string
 * @param pretty Whether the JSON is serialized with indentation or not. Default false as it's the best for storing data (less wasted space to hold the same data).
 */
@Suppress("UnusedReceiverParameter")
//It's used, it's how we resolve ID without needing to pass every type param at the use site; plus, only Exposed entities can have columns anyway.
inline fun <ID : Comparable<ID>, reified R> Entity<ID>.serializedJson(
    column: Column<String>, pretty: Boolean = false
): ParsedDataBackedProperty<ID, String, R> =
    serialized(column) {
        encoder { formatJsonString(it, pretty) }
        decoder { parseJsonString(it, pretty) }
    }

inline fun <reified T> Table.json(name: String, default: T? = null, collate: String? = null) =
    varchar(name, h2StringMaxLength, collate)
        .applyIf(default != null) {
            default(formatJsonString(default, false))
        }
