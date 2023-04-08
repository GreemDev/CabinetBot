package net.greemdev.cabinet.lib.util

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.json.Json

fun json(pretty: Boolean) = if (pretty) prettyJson else Json
fun<R> json(pretty: Boolean, block: Json.() -> R) = json(pretty).run(block)

val prettyJson = Json {
    prettyPrint = true
    encodeDefaults = true
}

/**
 * Shorthand for [PrimitiveSerialDescriptor] getting serial name from the fully qualified name of the provided type.
 * This function will produce a nullref if the type is a local class.
 */
inline fun<reified T> serialDescriptor(kind: PrimitiveKind) = PrimitiveSerialDescriptor(T::class.qualifiedName!!, kind)

/**
 * Shorthand for [PrimitiveSerialDescriptor] getting serial name from the fully qualified name of the current [KSerializer].
 */
fun KSerializer<*>.serializerDescriptor(kind: PrimitiveKind) = PrimitiveSerialDescriptor(this::class.qualifiedName!!, kind)


abstract class PrimitiveKSerializer<T>(primitiveKind: PrimitiveKind) : KSerializer<T> {
    override val descriptor: SerialDescriptor = serializerDescriptor(primitiveKind)
}
