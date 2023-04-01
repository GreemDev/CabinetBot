package net.greemdev.cabinet.lib.util

inline fun<T> taking(value: T, block: T.() -> Unit): T = value.apply(block)

inline fun<T> modifying(value: T, crossinline block: T.() -> Unit) = object : PropertyValue<T> {
    val resulting: T = taking(value, block)
    override fun get(thisRef: Any?): T = resulting
}

inline fun<T : Disposable> using(value: T, block: T.() -> Unit) {
    value.apply(block).also(Disposable::dispose)
}

suspend inline fun<T : AsyncDisposable> usingAsync(value: T, block: T.() -> Unit) {
    value.block()
    value.dispose()
}

data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

abstract class Disposable {
    open val disposed = false
    open fun dispose() { }
}

abstract class AsyncDisposable {
    open val disposed = false
    open suspend fun dispose() { }
}