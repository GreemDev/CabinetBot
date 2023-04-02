package net.greemdev.cabinet.lib.kordex

import com.kotlindiscord.kord.extensions.checks.types.CheckContextWithCache
import com.kotlindiscord.kord.extensions.utils.MutableStringKeyedMap
import com.kotlindiscord.kord.extensions.utils.getOf
import dev.kord.core.event.Event
import net.greemdev.cabinet.lib.util.invoking
import net.greemdev.cabinet.lib.util.string
import kotlin.reflect.KProperty

suspend fun<T : Event, V> CheckContextWithCache<T>.cacheNonnull(key: String, message: String? = null, block: suspend (T) -> V?) {
    createCheck {
        cacheNonnull(key, message, block)
    }
}

suspend fun<T : Event, V> CheckContextWithCache<T>.cacheNonnullThenGet(key: String, message: String? = null, block: suspend (T) -> V?): V {
    var result: V? = null
    createCheck {
        result = cacheNonnullThenGet(key, message, block)
    }
    return result!!
}

suspend fun<T : Event, V> CheckCreateScope<T>.cacheNonnullThenGet(key: String, message: String? = null, block: suspend (T) -> V?): V {
    val result = block(event)
    failIf(result == null, message)
    cacheContext.cache[key] = result!!
    return result
}

suspend fun<T : Event, V> CheckCreateScope<T>.cacheNonnull(key: String, message: String? = null, block: suspend (T) -> V?) {
    val result = block(event)
    failIf(result == null, message)
    cacheContext.cache[key] = result!!
}


class CacheException(message: String, cause: Throwable? = null) : Exception(message, cause)

inline fun <reified V> CheckContextWithCache<*>.getCached(key: () -> String): V {
    val k = key()
    return try {
        cache.getOf(k)
    } catch (e: ClassCastException) {
        throw CacheException("Cache value present at key $k, but not of type ${V::class.simpleName}", e)
    } catch (e: IllegalArgumentException) {
        throw CacheException("Cache does not have a value present at key $k", e)
    }
}

inline operator fun<reified V : Any> MutableStringKeyedMap<Any>.getValue(thisRef: Any?, property: KProperty<*>): V {
    check(size == 1) {
        "Cannot perform singleton get operation on cache with no or multiple entries."
    }
    return values.first() as? V ?: throw CacheException("Only item in cache was not of type ${V::class.simpleName}")
}

inline operator fun<reified V : Any> MutableStringKeyedMap<Any>.invoke(key: String) = invoking {
    try {
        getOf<V>(key)
    } catch (e: ClassCastException) {
        throw CacheException("Cache value present at key $key, but not of type ${V::class.simpleName}", e)
    } catch (e: IllegalArgumentException) {
        throw CacheException("Cache does not have a value present at key $key", e)
    }
}

inline operator fun<reified V : Any> MutableStringKeyedMap<Any>.invoke(key: () -> String) = this<V>(key())
