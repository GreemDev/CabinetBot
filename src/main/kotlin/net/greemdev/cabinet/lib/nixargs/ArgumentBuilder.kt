package net.greemdev.cabinet.lib.nixargs

import joptsimple.ArgumentAcceptingOptionSpec
import joptsimple.ValueConverter

class ArgumentBuilder<V>(val spec: ArgumentAcceptingOptionSpec<V>) {
    fun <T> usingConverter(aConverter: ValueConverter<T>?) {
        spec.withValuesConvertedBy(aConverter)
    }

    fun describedAs(description: String) {
        spec.describedAs(description)
    }

    fun valueSeparator(separator: Char) {
        spec.withValuesSeparatedBy(separator)
    }

    fun valueSeparator(separator: String) {
        spec.withValuesSeparatedBy(separator)
    }

    @SafeVarargs
    fun defaultsTo(value: V, vararg values: V) {
        spec.defaultsTo(value, *values)
    }

    fun defaultsTo(values: Array<V>) {
        spec.defaultsTo(values)
    }

    fun required() {
        spec.required()
    }
}