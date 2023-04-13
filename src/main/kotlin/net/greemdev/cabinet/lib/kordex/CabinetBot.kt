package net.greemdev.cabinet.lib.kordex

import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.builders.ExtensibleBotBuilder
import com.kotlindiscord.kord.extensions.utils.getKoin
import com.kotlindiscord.kord.extensions.utils.loadModule
import dev.kord.core.Kord
import dev.kord.core.behavior.getChannelOf
import dev.kord.core.entity.channel.TextChannel
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import net.greemdev.cabinet.botConfig
import org.koin.core.component.inject
import net.greemdev.cabinet.lib.nixargs.Options
import net.greemdev.cabinet.lib.util.*
import org.koin.dsl.bind
import kotlin.properties.ReadOnlyProperty

class CabinetBot(token: String, b: ExtensibleBotBuilder) : ExtensibleBot(b, token) {

    val kord: Kord by inject()

    suspend fun getPrismGuild() = kord.getGuildOrThrow(botConfig.guild)
    suspend fun getCabinetMembers() = getPrismGuild().members.filter { it.isCabinetMember }
    suspend fun getCabinetRole() = getPrismGuild().getRole(botConfig.cabinetRole)
    suspend fun getCabinetChannel() = getPrismGuild().getChannelOf<TextChannel>(botConfig.cabinetChannel)
    suspend fun getCurrentPresident() = getPrismGuild().members.first { botConfig.presidentRole in it.roleIds }

    override val logger = net.greemdev.cabinet.logger
    companion object : PropertyValue<CabinetBot> {
        override fun get(thisRef: Any?): CabinetBot = tryOrNull {
            koinComponent<CabinetBot>()
        } ?: error("The Discord bot has not logged in yet!")

        fun programArgs() = ::cli.getOrNull() ?: error("There are no command-line arguments set.")
        fun init(opt: Options) {
            cli = opt
        }

        private lateinit var cli: Options
    }
}

suspend fun buildCabinetBot(token: String, builder: suspend BotBuilder.() -> Unit) =
    BotBuilder().apply { builder() }.login(token)

class BotBuilder : ExtensibleBotBuilder() {

    suspend fun login(token: String) = build(token) as CabinetBot

    override suspend fun build(token: String): ExtensibleBot {
        hooksBuilder.beforeKoinSetup {
            if (pluginBuilder.enabled) {
                loadPlugins()
            }

            deferredExtensionsBuilders.forEach { it(extensionsBuilder) }
        }

        setupKoin()

        val bot = CabinetBot(token, this)

        loadModule {
            single { bot } bind ExtensibleBot::class
        }

        hooksBuilder.runCreated(bot)

        bot.setup()

        hooksBuilder.runSetup(bot)
        hooksBuilder.runBeforeExtensionsAdded(bot)

        extensionsBuilder.extensions.forEach {
            try {
                bot.addExtension(it)
            } catch (e: Exception) {
                logger.error(e) {
                    "Failed to set up extension $it"
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