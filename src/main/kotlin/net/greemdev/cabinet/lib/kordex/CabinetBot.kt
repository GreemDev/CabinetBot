package net.greemdev.cabinet.lib.kordex

import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.builders.ExtensibleBotBuilder
import com.kotlindiscord.kord.extensions.utils.getKoin
import com.kotlindiscord.kord.extensions.utils.loadModule
import dev.kord.core.Kord
import org.koin.core.component.inject
import net.greemdev.cabinet.lib.nixargs.Options
import net.greemdev.cabinet.lib.util.getOrNull
import net.greemdev.cabinet.lib.util.slf4j
import net.greemdev.cabinet.lib.util.tryOrNull
import org.koin.dsl.bind

class CabinetBot(token: String, b: ExtensibleBotBuilder) : ExtensibleBot(b, token) {

    val kord: Kord by inject()

    override val logger = Companion.logger
    companion object {
        val logger by slf4j { CabinetBot::class }

        fun programArgs() = ::cli.getOrNull() ?: error("There are no command-line arguments set.")
        fun init(opt: Options) {
            cli = opt
        }

        private lateinit var cli: Options
        operator fun invoke() = tryOrNull { getKoin().get<CabinetBot>() } ?: error("The Discord bot has not logged in yet!")
    }
}

suspend fun buildCabinetBot(token: String, builder: suspend BotBuilder.() -> Unit) =
    BotBuilder().apply { builder() }.buildBot(token)

class BotBuilder : ExtensibleBotBuilder() {

    suspend fun buildBot(token: String) = build(token) as CabinetBot

    override suspend fun build(token: String): ExtensibleBot {
        hooksBuilder.beforeKoinSetup {
            if (pluginBuilder.enabled) {
                loadPlugins()
            }

            deferredExtensionsBuilders.forEach { it(extensionsBuilder) }
        }

        setupKoin()

        val bot = CabinetBot(token, this)

        loadModule { single { bot } bind ExtensibleBot::class }

        hooksBuilder.runCreated(bot)

        bot.setup()

        hooksBuilder.runSetup(bot)
        hooksBuilder.runBeforeExtensionsAdded(bot)

        extensionsBuilder.extensions.forEach {
            try {
                bot.addExtension(it)
            } catch (e: Exception) {
                logger.error(e) {
                    "Failed to set up extension: $it"
                }
            }
        }

        if (pluginBuilder.enabled) {
            startPlugins()
        }

        hooksBuilder.runAfterExtensionsAdded(bot)

        return bot
    }
}