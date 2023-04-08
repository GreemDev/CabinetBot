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
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.*

fun <ID : Comparable<ID>, T : Entity<ID>> EntityClass<ID, T>.findOptional(id: ID?): Optional<T> {
    return findById(id ?: return optionalOf()).toOptional()
}

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
) = DataPropertyBuilder.forColumn<ID, S, R>(column).apply(initializer).build()

/**
 * A property delegate for a kotlinx [Instant] whose value is represented in the Exposed [column] as a [Long] value.
 */
fun <ID : Comparable<ID>> serializedInstant(column: Column<Long>) = ParsedDataBackedProperty.instant<ID>(column)

/**
 * A property delegate for a kord [Snowflake] whose value is represented in the Exposed [column] as a [ULong] value.
 */
fun <ID : Comparable<ID>> serializedSnowflake(column: Column<ULong>) = ParsedDataBackedProperty.snowflake<ID>(column)

/**
 * A property delegate for a Kotlin object whose value is represented in the Exposed [column] as a compound JSON string value.
 * The getter parses the underlying data, and the setter sets the underlying data to its newer counterpart, as JSON.
 * @param column The varchar column whose value should be treated as a JSON string
 * @param pretty Whether the JSON is serialized with indentation or not. Default false as it's the best for storing data (less wasted space to hold the same data).
 */
@Suppress("UnusedReceiverParameter") //It's used, it's how we resolve ID without needing to pass every type param at the use site; plus, only Exposed entities can have columns anyway.
inline fun <ID : Comparable<ID>, reified R> Entity<ID>.serializedJson(
        column: Column<String>,
        pretty: Boolean = false
) = ParsedDataBackedProperty.json<ID, R>(column, pretty)


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
    // "connects" by accessing BotDatabase.INSTANCE (the internal reference to the singleton kotlin object BotDatabase) when compiled, which calls the above init block.
    infix fun <T> start(block: suspend Transaction.() -> T) = runBlocking { newSuspendedTransaction { block() } }
}

fun <ID : Comparable<ID>, T : Entity<ID>> EntityClass<ID, T>.listAll(): List<T> = all().toList()

fun <T> Column<T>.defaultValue(): Optional<T> = optionalOf(defaultValueFun).map { it() }

fun Transaction.addCustomLogger(logger: StatementContext.(Transaction) -> Unit) = addLogger(object : SqlLogger {
    override fun log(context: StatementContext, transaction: Transaction) {
        context.logger(transaction)
    }
})

const val h2StringMaxLength = 1048576

fun Table.varchar(name: String, collate: String? = null) = varchar(name, h2StringMaxLength, collate)

inline fun <reified T> Table.json(name: String, default: T? = null, collate: String? = null) =
        varchar(name, collate)
                .applyIf(default != null) {
                    default(formatJsonString(default, false))
                }