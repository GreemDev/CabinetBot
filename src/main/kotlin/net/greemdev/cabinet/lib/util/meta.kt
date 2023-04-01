package net.greemdev.cabinet.lib.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import java.util.*
import kotlin.time.Duration

typealias static = JvmStatic

fun Optional<Boolean>.orFalse() = orNull().orFalse()
fun Boolean?.orFalse() = this ?: false

inline fun <reified T> parseJsonString(json: String, pretty: Boolean = true): T =
    json(pretty) { decodeFromString(json) }

inline fun<reified T> formatJsonString(src: T, pretty: Boolean = true): String =
    json(pretty) { encodeToString(src) }

fun <T> tryOrNull(func: () -> T): T? = try {
    func()
} catch (_: Throwable) {
    null
}

suspend fun trySuspend(func: suspend () -> Unit) {
    try {
        func()
    } catch (_: Throwable) {
    }
}

fun <T> tryOrNullAsync(func: suspend CoroutineScope.() -> T): Deferred<T?> = scope.async {
    try {
        func()
    } catch (_: Throwable) {
        null
    }
}

fun <T> T.applyIf(condition: Boolean, func: T.() -> Unit): T {
    if (condition)
        apply(func)
    return this
}

fun duration(func: Duration.Companion.() -> Duration) = func(Duration)

fun<T> Optional<T>.orNull(): T? = orElse(null)
fun<T> T?.toOptional() = if (this == null) Optional.empty() else Optional.of(this)
fun <T> optionalOf(value: T? = null) = value.toOptional()
