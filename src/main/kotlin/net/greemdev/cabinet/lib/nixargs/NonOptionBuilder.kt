package net.greemdev.cabinet.lib.nixargs

import joptsimple.ArgumentAcceptingOptionSpec
import joptsimple.NonOptionArgumentSpec
import joptsimple.ValueConverter

class NonOptionBuilder<V>(val spec: NonOptionArgumentSpec<V>) {
    fun <T> usingConverter(aConverter: ValueConverter<T>?) {
        spec.withValuesConvertedBy(aConverter)
    }

    fun describedAs(description: String) {
        spec.describedAs(description)
    }
}