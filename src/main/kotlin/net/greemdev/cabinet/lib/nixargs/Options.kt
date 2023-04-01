package net.greemdev.cabinet.lib.nixargs

import joptsimple.*
import net.greemdev.cabinet.lib.util.invoking
import net.greemdev.cabinet.lib.util.tryOrNull
import java.util.*

data class Options(private val os: OptionSet) {
    fun any() = os.hasOptions()
    operator fun contains(option: String) = os.has(option)
    operator fun contains(option: OptionSpec<*>): Boolean = os.has(option)

    fun hasArguments(option: String) = os.hasArgument(option)
    fun hasArguments(option: OptionSpec<*>) = os.hasArgument(option)

    operator fun get(option: String) =
        Option(
            try {
                os.valueOf(option)
            } catch (_: Throwable) {
                os.valuesOf(option)
            }
        )


    operator fun <V> get(spec: OptionSpec<V>) =
        Option(
            try {
                os.valueOf(spec)
            } catch (_: Throwable) {
                os.valuesOf(spec)
            }
        )


    val specs by invoking(os::specs)
    val nonOptions by invoking(os::nonOptionArguments)

    fun map(): Map<OptionSpec<*>, List<*>> = os.asMap()

    override fun equals(other: Any?) = os == other

    override fun hashCode() = os.hashCode()
}

data class Option(val data: Any?) {
    inline fun <reified V> getAs() =
        data as? V ?: error("Data is of type ${data?.javaClass?.simpleName}, not ${V::class.simpleName}.")

    @Suppress("UNCHECKED_CAST")
    inline fun <reified V> asList() =
        data as? List<V> ?: error("Data is of type ${data?.javaClass?.simpleName}, not List<${V::class.simpleName}>")
}