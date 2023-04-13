package net.greemdev.cabinet.database.x

import dev.kord.common.entity.Snowflake
import kotlinx.datetime.Instant
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.sql.Column
import net.greemdev.cabinet.lib.util.*
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

private val missingTransformersErrorMessage by lazy {
    "${ParsedDataBackedProperty::class.simpleName} cannot be created without both encoding and decoding functions."
}

abstract class DataPropertyBuilder<ID : Comparable<ID>, S, R>(private val column: Column<S>) {
    companion object {
        fun <ID : Comparable<ID>, S, R> forColumn(column: Column<S>) =
            object : DataPropertyBuilder<ID, S, R>(column) {}
    }

    private var encoder: Parser<R, S>? = null
    private var decoder: Parser<S, R>? = null

    infix fun encoder(encoder: Parser<R, S>): DataPropertyBuilder<ID, S, R> {
        this.encoder = encoder
        return this
    }

    infix fun decoder(decoder: Parser<S, R>): DataPropertyBuilder<ID, S, R> {
        this.decoder = decoder
        return this
    }

    infix fun encoder(block: (R) -> S) = encoder(Parser(block))
    infix fun decoder(block: (S) -> R) = decoder(Parser(block))

    fun build() = ParsedDataBackedProperty<ID, S, R>(
        column,
        checkNotNull(decoder, ::missingTransformersErrorMessage),
        checkNotNull(encoder, ::missingTransformersErrorMessage)
    )
}

open class ParsedDataBackedProperty<ID : Comparable<ID>, S, R>(
    private val column: Column<S>,
    private val fromData: Parser<S, R>,
    private val toData: Parser<R, S>
) : ReadWriteProperty<Entity<ID>, R> {
    override fun getValue(thisRef: Entity<ID>, property: KProperty<*>): R =
        thisRef.run {
            fromData(column.getValue(this, property))
        }

    override fun setValue(thisRef: Entity<ID>, property: KProperty<*>, value: R) {
        thisRef.run {
            column.setValue(this, property, toData(value))
        }
    }

    companion object {
        fun <ID : Comparable<ID>> snowflake(column: Column<ULong>) =
            ParsedDataBackedProperty<ID, ULong, Snowflake>(
                column,
                Parser { it.snowflake },
                Parser { it.value }
            )

        fun <ID : Comparable<ID>> instant(column: Column<Long>) =
            ParsedDataBackedProperty<ID, Long, Instant>(
                column,
                Parser { Instant.fromEpochMilliseconds(it) },
                Parser { it.toEpochMilliseconds() }
            )

        inline fun <ID : Comparable<ID>, reified R> json(column: Column<String>) =
            ParsedDataBackedProperty<ID, String, R>(
                column,
                Parser { parseJsonString(it, false) },
                Parser { formatJsonString(it, false) }
            )
    }

}