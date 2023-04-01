plugins {
    kotlin("jvm")
    kotlin("plugin.serialization") version versions.kotlin
    id("com.github.johnrengelman.shadow") version versions.shadow
    application
}

group = "net.greemdev.cabinet"
version = versions.project

repositories {
    mavenCentral()
    maven(repo.sonatype)
}

application {
    mainClass.set("net.greemdev.cabinet.AppKt")
}

dependencies.accumulate {
    Dependencies {
        +logback
        +kordEx
        +kordEmoji
        +h2
        +joptSimple
        +kotlin
        +exposed
    }
}

kotlin {
    sourceSets.all {
        languageSettings {
            ktLangFeatures.forEach { (f, condition) ->
                if (condition()) enableLanguageFeature(f)
            }
        }
    }
}

tasks {
    compileKotlin {
        kotlinOptions {
            jvmTarget = versions.jvm
            compilerArgs {
                optIns()
                preview.contextReceivers()
            }
        }
    }

    shadowJar {
        archiveFileName.set("Cabinet-Bot-$version.jar")
    }
}