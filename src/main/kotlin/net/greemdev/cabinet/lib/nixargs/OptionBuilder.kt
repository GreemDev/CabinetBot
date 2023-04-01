package net.greemdev.cabinet.lib.nixargs

import joptsimple.*

class OptionBuilder(spec: OptionSpecBuilder) {
    var spec: OptionSpecBuilder
        private set

    init {
        this.spec = spec
    }

    inline fun<reified A> requiredArgOfType(func: ArgumentBuilder<A>.() -> Unit) {
        func(ArgumentBuilder(spec.withRequiredArg().ofType(A::class.java)))
    }

    inline fun<reified A> optionalArgOfType(func: ArgumentBuilder<A>.() -> Unit) {
        func(ArgumentBuilder(spec.withOptionalArg().ofType(A::class.java)))
    }

    fun requiredArg(func: ArgumentBuilder<String>.() -> Unit) {
        func(ArgumentBuilder(spec.withRequiredArg()))
    }

    fun optionalArg(func: ArgumentBuilder<String>.() -> Unit) {
        func(ArgumentBuilder(spec.withOptionalArg()))
    }

    fun requiredIf(dependent: String, vararg otherDependents: String?) {
        spec.requiredIf(dependent, *otherDependents)
    }

    fun requiredIf(dependent: OptionSpec<*>, vararg otherDependents: OptionSpec<*>) {
        spec.requiredIf(dependent, *otherDependents)
    }

    fun requiredUnless(dependent: String, vararg otherDependents: String) {
        spec.requiredUnless(dependent, *otherDependents)
    }

    fun requiredUnless(dependent: OptionSpec<*>, vararg otherDependents: OptionSpec<*>) {
        spec.requiredUnless(dependent, *otherDependents)
    }

    fun availableIf(dependent: String, vararg otherDependents: String) {
        spec.availableIf(dependent, *otherDependents)
    }

    fun availableIf(dependent: OptionSpec<*>, vararg otherDependents: OptionSpec<*>) {
        spec.availableIf(dependent, *otherDependents)
    }

    fun availableUnless(dependent: String, vararg otherDependents: String?) {
        spec.availableUnless(dependent, *otherDependents)
    }

    fun availableUnless(dependent: OptionSpec<*>, vararg otherDependents: OptionSpec<*>) {
        spec.availableUnless(dependent, *otherDependents)
    }
}