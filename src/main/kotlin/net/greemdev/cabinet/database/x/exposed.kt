package net.greemdev.cabinet.database.x

import dev.kord.common.entity.Snowflake
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import net.greemdev.cabinet.lib.util.*
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.StatementContext

fun <ID : Comparable<ID>, T : Entity<ID>> EntityClass<ID, T>.findOptional(id: ID?) =
    if (id == null)
        optionalOf()
    else
        findById(id).toOptional()

fun <T : Entity<ULong>> EntityClass<ULong, T>.findBySnowflake(id: Snowflake?) = findOptional(id?.value)

/**
 * A property delegate for a Kotlin object whose value is represented in the Exposed [column] as a [S] (source) value;
 * specifying functions for parsing the data into a value of [R] and serializing the result [R] back into its original value [S].
 * The most common usage of this is when S is [String] for parsing; allowing usage such as compound JSON objects as column values.
 * The getter parses the underlying data, and the setter sets the underlying data to its newer counterpart, all data passed through the specified functions.
 * @param column The data column whose value should be treated as parsable data.
 * @exception IllegalStateException thrown when the property builder does not have one or both of the value converters.
 */
fun <ID : Comparable<ID>, S, R> serialized(
    column: Column<S>,
    initializer: DataPropertyBuilder<ID, S, R>.() -> Unit
) = DataPropertyBuilder.createNew<ID, S, R>(column).apply(initializer).build()

/**
 * A property delegate for a kotlinx [Instant] whose value is represented in the Exposed [column] as a [Long] value.
 */
fun <ID : Comparable<ID>> serializedInstant(
    column: Column<Long>
) = serialized<ID, Long, Instant>(column) {
    encoder { it.toEpochMilliseconds() }
    decoder { Instant.fromEpochMilliseconds(it) }
}

/**
 * A property delegate for a kord [Snowflake] whose value is represented in the Exposed [column] as a [ULong] value.
 */
fun <ID : Comparable<ID>> serializedSnowflake(
    column: Column<ULong>
) = serialized<ID, ULong, Snowflake>(column) {
    encoder { it.value }
    decoder { it.snowflake }
}


/**
 * Identity table with Discord Snowflake ([ULong]) primary key
 *
 * @param name table name, by default name will be resolved from a class name with "Table" suffix removed (if present)
 * @param columnName name for a primary key, "id" by default
 */
open class SnowflakeIdTable(name: String = "", columnName: String = "id") : IdTable<ULong>(name) {
    final override val id = ulong(columnName).entityId()
    override val primaryKey = PrimaryKey(id)
}

object BotDatabase {

    init {
        Database.connect("jdbc:h2:./data/cabinet")
    }

    /**
     * Connects to the database and runs the provided [block] transaction afterwards.
     */
    // "connects" by accessing BotDatabase.INSTANCE when compiled, which calls the above init block.
    infix fun <T> start(block: suspend Transaction.() -> T) = runBlocking { transactionAsync { block() }.await() }
}

fun <ID : Comparable<ID>, T : Entity<ID>> EntityClass<ID, T>.listAll(): List<T> = all().toList()

fun <T> Column<T>.default(): T = defaultOrNull() ?: error("Column does not have a default value.")
fun <T> Column<T>.defaultOrNull(): T? = optionalOf(defaultValueFun).map { it() }.orNull()

fun Transaction.addCustomLogger(logger: StatementContext.(Transaction) -> Unit) = addLogger(object : SqlLogger {
    override fun log(context: StatementContext, transaction: Transaction) {
        context.logger(transaction)
    }
})

const val h2StringMaxLength = 1048576

fun Table.varchar(name: String, collate: String? = null) = varchar(name, h2StringMaxLength, collate)