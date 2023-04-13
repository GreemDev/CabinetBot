package net.greemdev.cabinet.lib.nixargs

import joptsimple.NonOptionArgumentSpec
import joptsimple.ValueConverter

class NonOptionBuilder<V>(val spec: NonOptionArgumentSpec<V>) {
    fun <T> usingConverter(aConverter: ValueConverter<T>?): NonOptionBuilder<V> {
        spec.withValuesConvertedBy(aConverter)
        return this
    }

    fun describedAs(description: String): NonOptionBuilder<V> {
        spec.describedAs(description)
        return this
    }
}