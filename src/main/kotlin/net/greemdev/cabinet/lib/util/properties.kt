package net.greemdev.cabinet.lib.util

import mu.KotlinLogging
import kotlin.properties.*
import kotlin.reflect.*

fun <T> invoking(func: () -> T) = getting<Any?, T> { func() }
fun <T> invokingOrNull(func: () -> T) = invoking { tryOrNull(func) }
fun slf4j(fn: () -> Any) = lazy {
    when (val loggerName = fn()) {
        is Class<*> -> KotlinLogging.logger(loggerName.simpleName)
        is KClass<*> -> KotlinLogging.logger(loggerName.simpleName ?: "Anonymous")
        else -> KotlinLogging.logger(loggerName.toString())
    }
}
fun<T> KProperty0<T>.hasValue() = isLateinit && getOrNull() != null
fun<T> KProperty0<T>.hasNoValue() = isLateinit && getOrNull() == null
fun<T> KProperty0<T>.getOrNull(): T? = tryOrNull { get() }

fun<T> KProperty0<T>.runOrNull(block: (T) -> Unit): T?
    = optionalOf(getOrNull()).also { it.ifPresent(block) }.orNull()

fun<C, R> getting(func: C.(KProperty<*>) -> R) =
    ReadOnlyProperty<C, R> { thisRef, property -> func(thisRef, property) }

suspend fun<T> KProperty0<T>.suspendOrNull(block: suspend (T) -> Unit): T? = getOrNull()?.also { block(it) }


fun regexString(options: Collection<RegexOption>, pattern: () -> CharSequence) = RegexProperty(options.toSet(), pattern().string())
fun regexString(vararg options: RegexOption, pattern: () -> CharSequence) = RegexProperty(options.toSet(), pattern().string())

interface PropertyValue<T> : ReadOnlyProperty<Any?, T> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): T = get(thisRef)
    fun get(thisRef: Any?): T
}
interface MutablePropertyValue<TReceiver, T> : ReadWriteProperty<TReceiver, T> {
    override fun getValue(thisRef: TReceiver, property: KProperty<*>): T = get(thisRef)
    override fun setValue(thisRef: TReceiver, property: KProperty<*>, value: T) = set(thisRef, value)
    fun get(thisRef: TReceiver): T
    fun set(thisRef: TReceiver, value: T)
}

data class RegexProperty(private val options: Set<RegexOption> = setOf(), private val pattern: String) : PropertyValue<Regex> {
    val lazyRegex by lazy {
        pattern.toRegex(options)
    }
    override fun get(thisRef: Any?): Regex = lazyRegex
}