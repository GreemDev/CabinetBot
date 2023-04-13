package net.greemdev.cabinet.lib.nixargs

import joptsimple.ArgumentAcceptingOptionSpec
import joptsimple.ValueConverter

class ArgumentBuilder<V>(val spec: ArgumentAcceptingOptionSpec<V>) {
    fun <T> usingConverter(aConverter: ValueConverter<T>): ArgumentBuilder<V> {
        spec.withValuesConvertedBy(aConverter)
        return this
    }

    fun describedAs(description: String): ArgumentBuilder<V>  {
        spec.describedAs(description)
        return this
    }

    fun valueSeparator(separator: Char): ArgumentBuilder<V>  {
        spec.withValuesSeparatedBy(separator)
        return this
    }

    fun valueSeparator(separator: String): ArgumentBuilder<V>  {
        spec.withValuesSeparatedBy(separator)
        return this
    }

    @SafeVarargs
    fun defaultsTo(value: V, vararg values: V): ArgumentBuilder<V>  {
        spec.defaultsTo(value, *values)
        return this
    }

    fun defaultsTo(values: Array<V>): ArgumentBuilder<V>  {
        spec.defaultsTo(values)
        return this
    }

    fun required(): ArgumentBuilder<V>  {
        spec.required()
        return this
    }
}