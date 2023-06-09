package net.greemdev.cabinet.lib.nixargs

import joptsimple.*
import java.util.*

fun optionParser(shortSpec: String? = null, func: OptionParserBuilder.() -> Unit) =
        OptionParserBuilder(
                shortSpec?.let {
                    OptionParser(shortSpec)
                } ?: OptionParser()
        ).apply(func).op

open class OptionParserBuilder(val op: OptionParser) {
    fun option(name: String, builder: OptionBuilder.() -> Unit = {}) =
            OptionBuilder(op.accepts(name)).builder()

    fun option(vararg names: String, builder: OptionBuilder.() -> Unit = {}) =
            OptionBuilder(op.acceptsAll(names.toMutableList())).builder()

    fun option(name: String, description: String, builder: OptionBuilder.() -> Unit = {}) =
            OptionBuilder(op.accepts(name, description)).builder()

    fun option(vararg names: String, description: String, builder: OptionBuilder.() -> Unit = {}) =
            OptionBuilder(op.acceptsAll(names.toMutableList(), description)).builder()

    fun nonOptions(description: String, builder: NonOptionBuilder<*>.() -> Unit) =
            NonOptionBuilder(op.nonOptions(description)).builder()

    inline fun <reified V> nonOptionsOf(description: String, builder: NonOptionBuilder<V>.() -> Unit) =
            NonOptionBuilder(op.nonOptions(description).ofType(V::class.java)).builder()

    fun nonOptions(builder: NonOptionBuilder<*>.() -> Unit) =
            NonOptionBuilder(op.nonOptions()).builder()

    inline fun <reified V> nonOptionsOf(builder: NonOptionBuilder<V>.() -> Unit) =
            NonOptionBuilder(op.nonOptions().ofType(V::class.java)).builder()
}