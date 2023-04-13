package net.greemdev.cabinet.lib.nixargs

import joptsimple.*

class OptionBuilder(val spec: OptionSpecBuilder) {

    inline fun<reified A> requiredArgOfType(func: ArgumentBuilder<A>.() -> Unit): OptionBuilder {
        func(ArgumentBuilder(spec.withRequiredArg().ofType(A::class.java)))
        return this
    }

    inline fun<reified A> optionalArgOfType(func: ArgumentBuilder<A>.() -> Unit): OptionBuilder {
        func(ArgumentBuilder(spec.withOptionalArg().ofType(A::class.java)))
        return this
    }

    fun requiredArg(func: ArgumentBuilder<String>.() -> Unit): OptionBuilder {
        func(ArgumentBuilder(spec.withRequiredArg()))
        return this
    }

    fun optionalArg(func: ArgumentBuilder<String>.() -> Unit): OptionBuilder {
        func(ArgumentBuilder(spec.withOptionalArg()))
        return this
    }

    fun requiredIf(dependent: String, vararg otherDependents: String?): OptionBuilder {
        spec.requiredIf(dependent, *otherDependents)
        return this
    }

    fun requiredIf(dependent: OptionSpec<*>, vararg otherDependents: OptionSpec<*>): OptionBuilder {
        spec.requiredIf(dependent, *otherDependents)
        return this
    }

    fun requiredUnless(dependent: String, vararg otherDependents: String): OptionBuilder {
        spec.requiredUnless(dependent, *otherDependents)
        return this
    }

    fun requiredUnless(dependent: OptionSpec<*>, vararg otherDependents: OptionSpec<*>): OptionBuilder {
        spec.requiredUnless(dependent, *otherDependents)
        return this
    }

    fun availableIf(dependent: String, vararg otherDependents: String): OptionBuilder {
        spec.availableIf(dependent, *otherDependents)
        return this
    }

    fun availableIf(dependent: OptionSpec<*>, vararg otherDependents: OptionSpec<*>): OptionBuilder {
        spec.availableIf(dependent, *otherDependents)
        return this
    }

    fun availableUnless(dependent: String, vararg otherDependents: String?): OptionBuilder {
        spec.availableUnless(dependent, *otherDependents)
        return this
    }

    fun availableUnless(dependent: OptionSpec<*>, vararg otherDependents: OptionSpec<*>): OptionBuilder {
        spec.availableUnless(dependent, *otherDependents)
        return this
    }
}