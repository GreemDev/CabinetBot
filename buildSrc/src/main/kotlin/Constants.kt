@file:Suppress("ClassName")

import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions

val ktLangFeatures = mapOf(
    "ExplicitBackingFields" to { versions.isKt17() }
)


object Dependencies {

    operator fun invoke(block: Dependencies.() -> Unit) = block(this)

    val logback = "ch.qos.logback" artifact "logback-classic" version versions.logback
    val kordEmoji = "dev.kord.x" artifact "emoji" version versions.kordEmoji
    val kordEx = "com.kotlindiscord.kord.extensions" artifact "kord-extensions" version versions.kordEx
    val h2 = "com.h2database" artifact "h2" version versions.h2
    val joptSimple = "net.sf.jopt-simple" artifact "jopt-simple" version versions.joptSimple
    val exposed = with(groups.exposed) { setOf(core, jdbc, dao) }
    val kotlin = with(groups.kotlin) { setOf(reflect, coroutines) }

    private object groups {
        object exposed {
            val core = exposed("core")
            val jdbc = exposed("jdbc")
            val dao = exposed("dao")
        }
        object kotlin {
            val reflect = kt("reflect")
            val coroutines = ktx("coroutines-core", versions.coroutines)
        }
    }
}

object versions {
    fun isKt17() = kotlin.startsWith("1.7")
    const val project = "1.0.0"
    const val jvm = "11"
    const val shadow = "7.1.2"
    const val kotlin = "1.8.20"
    const val coroutines = "1.6.4"
    const val exposed = "0.41.1"
    const val kordEmoji = "0.5.0"
    const val kordEx = "1.5.6"
    const val joptSimple = "6.0-alpha-3"
    const val h2 = "2.1.214"
    const val logback = "1.3.0"
}

object repo {
    const val sonatype = "https://oss.sonatype.org/content/repositories/snapshots/"
}

abstract class CompilerArguments(private val jvmOptions: KotlinJvmOptions) {
    val optIns = arrayOf(
        "kotlin.RequiresOptIn",
        "kotlin.ExperimentalUnsignedTypes",
        "kotlinx.coroutines.FlowPreview",
        "dev.kord.gateway.PrivilegedIntent",
        "dev.kord.common.annotation.KordPreview",
        "dev.kord.common.annotation.KordExperimental"
    )

    val preview by lazy(::PreviewFeatures)

    inner class PreviewFeatures {
        fun contextReceivers() = jvmOptions.appendCompilerArgs("-Xcontext-receivers")
        fun useK2() = jvmOptions.appendCompilerArgs("-Xuse-k2")
    }
    fun optIns() = jvmOptions.appendCompilerArgs(*optIns.map { "-opt-in=$it" }.toTypedArray())
}

fun KotlinJvmOptions.compilerArgs(block: CompilerArguments.() -> Unit)
        = object : CompilerArguments(this) {}.block()

fun KotlinJvmOptions.appendCompilerArgs(vararg args: String) = args.forEach {
    freeCompilerArgs = freeCompilerArgs + it
}