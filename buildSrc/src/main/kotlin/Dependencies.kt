import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.kotlin.dsl.DependencyHandlerScope
import org.gradle.kotlin.dsl.accessors.runtime.addExternalModuleDependencyTo

data class Dependency(
    var group: String, var artifact: String = "", var version: String = "",
    var dependencyConfiguration: (ExternalModuleDependency.() -> Unit)? = null
) {
    infix fun artifact(name: String) = apply {
        this.artifact = name
    }

    infix fun version(version: String) = apply {
        this.version = version
    }

    infix fun and(dependencyConfiguration: ExternalModuleDependency.() -> Unit) = apply {
        this.dependencyConfiguration = dependencyConfiguration
    }
}

infix fun String.artifact(name: String): Dependency = Dependency(this) artifact name

fun DependencyHandlerScope.implementation(vararg dependencies: Dependency) {
    dependencies.forEach {
        addExternalModuleDependencyTo(
            this, "implementation",
            it.group, it.artifact, it.version,
            null, null, null, it.dependencyConfiguration
        )
    }
}

fun kt(module: String, dependencyConfiguration: ExternalModuleDependency.() -> Unit = {}): Dependency = "org.jetbrains.kotlin" artifact "kotlin-$module" version versions.kotlin and dependencyConfiguration
fun ktx(module: String, version: String = versions.kotlin): Dependency = "org.jetbrains.kotlinx" artifact "kotlinx-$module" version version
fun exposed(module: String): Dependency = "org.jetbrains.exposed" artifact "exposed-$module" version versions.exposed

fun DependencyHandler.accumulate(func: Accumulator<Dependency>.(DependencyHandlerScope) -> Unit) {
    with(DependencyHandlerScope.of(this)) scope@{
        implementation(*Accumulator<Dependency>()
            .apply { func(this@scope) }
            .collection.toTypedArray())
    }
}

data class Accumulator<T>(val collection: MutableCollection<T> = arrayListOf()) {
    operator fun Collection<T>.unaryPlus() {
        collection.addAll(this)
    }
    operator fun Array<T>.unaryPlus() {
        +toSet()
    }
    operator fun T.unaryPlus() {
        collection.add(this)
    }
    operator fun Collection<T>.unaryMinus() {
        collection.removeAll(this.toSet())
    }
    operator fun Array<T>.unaryMinus() {
        -toSet()
    }
    operator fun T.unaryMinus() {
        collection.remove(this)
    }
}