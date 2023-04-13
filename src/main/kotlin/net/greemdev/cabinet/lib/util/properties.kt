package net.greemdev.cabinet.lib.util

import kotlin.properties.*
import kotlin.reflect.*

fun <T> invoking(func: () -> T) = getting<Any?, T> { func() }
fun <T> invokingOrNull(func: () -> T) = invoking { tryOrNull(func) }

fun<T> KProperty0<T>.getOrNull(): T? = tryOrNull { get() }
fun<T> KProperty0<T>.getCatching(): Result<T> = runCatching { get() }

fun<T> KProperty0<T>.runOrNull(block: (T) -> Unit): T?
    = optionalOf(getOrNull()).also { it.ifPresent(block) }.orNull()

fun<C, R> getting(func: C.(KProperty<*>) -> R) =
    ReadOnlyProperty<C, R> { thisRef, property -> func(thisRef, property) }

suspend fun<T> KProperty0<T>.suspendOrNull(block: suspend (T) -> Unit): T? = getOrNull()?.also { block(it) }

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