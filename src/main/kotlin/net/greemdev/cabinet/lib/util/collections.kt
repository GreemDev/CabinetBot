package net.greemdev.cabinet.lib.util

import kotlin.properties.ReadOnlyProperty
import kotlinx.serialization.Serializable
import java.util.Optional
import kotlin.reflect.KProperty

inline fun<reified T> Iterable<T>.array() = toSet().toTypedArray()

fun<T> accumulate(func: AccumulatorFunc<T>): Collection<T> = mutableListOf<T>().accumulate(func)
infix fun<T> Collection<T>.accumulate(func: AccumulatorFunc<T>): Collection<T> = Accumulator(toMutableList()).apply(func).collection

typealias AccumulatorFunc<T> = Accumulator<T>.() -> Unit
open class Accumulator<T>(val collection: MutableCollection<T>) {
    operator fun Collection<T>.unaryPlus() {
        collection.addAll(this)
    }
    operator fun T.unaryPlus() {
        collection.add(this)
    }
    operator fun Collection<T>.unaryMinus() {
        collection.removeAll(toSet())
    }
    operator fun T.unaryMinus() {
        collection.remove(this)
    }
}

abstract class SingletonList<T>(private val values: MutableCollection<T> = mutableSetOf()): Collection<T> by values {
    protected fun register(value: T): T = value.also(values::add)
    protected fun register(supplier: () -> T): T = supplier().also(values::add)
    protected fun registering(value: T) = Property { value }
    protected fun registering(supplier: () -> T) = Property(supplier)

    protected inner class Property(supplier: () -> T) : ReadOnlyProperty<SingletonList<T>, T> {
        private var cachedValue: T
        init {
            cachedValue = supplier()
            values.add(cachedValue)
        }
        override fun getValue(thisRef: SingletonList<T>, property: KProperty<*>): T = cachedValue
    }
}