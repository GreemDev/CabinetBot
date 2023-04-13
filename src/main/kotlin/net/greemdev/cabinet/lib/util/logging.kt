@file:Suppress("unused")
package net.greemdev.cabinet.lib.util

import mu.KLogger
import mu.KotlinLogging
import kotlin.reflect.KClass

fun slf4j(fn: () -> Any) = lazy {
    when (val loggerName = fn()) {
        is Class<*> -> KotlinLogging.logger(loggerName.simpleName)
        is KClass<*> -> KotlinLogging.logger(loggerName.simpleName ?: "Anonymous")
        else -> KotlinLogging.logger(loggerName.toString())
    }
}

fun slf4j(classOrName: Any) = slf4j(const(classOrName))
inline fun<reified T> slf4j() = slf4j(T::class)

fun logger(clazz: Class<*>) = KotlinLogging.logger(clazz.simpleName)
inline fun <reified T> logger() = logger(T::class)
fun logger(name: String? = null) = name?.let { KotlinLogging.logger(name) } ?: net.greemdev.cabinet.logger
fun logger(klass: KClass<*>) = KotlinLogging.logger(klass.simpleName ?: "Anon")
fun logger(klass: KClass<*>, func: KLogger.() -> Unit) = logger(klass).func()

operator fun KLogger.invoke(block: KLogger.() -> Unit) = apply(block)

infix fun KLogger.info(block: StringScope.() -> Unit) = info { string(builderScope = block) }
infix fun KLogger.trace(block: StringScope.() -> Unit) = trace { string(builderScope = block) }
infix fun KLogger.debug(block: StringScope.() -> Unit) = debug { string(builderScope = block) }
infix fun KLogger.warn(block: StringScope.() -> Unit) = warn { string(builderScope = block) }
infix fun KLogger.error(block: StringScope.() -> Unit) = error { string(builderScope = block) }

