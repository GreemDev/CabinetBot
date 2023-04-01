/*package net.greemdev.cabinet.extensions.welcome

import com.kotlindiscord.kord.extensions.extensions.event
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.getChannelOf
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.event.guild.MemberJoinEvent
import dev.kord.core.event.guild.MemberLeaveEvent
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import net.greemdev.cabinet.database.entities.GuildSetting
import net.greemdev.cabinet.database.entities.json.column.WelcomeData
import net.greemdev.cabinet.database.x.findBySnowflake
import net.greemdev.cabinet.lib.kordex.*
import net.greemdev.cabinet.lib.util.createEmbedOrNull
import net.greemdev.cabinet.lib.util.orNull
import net.greemdev.cabinet.lib.util.tryOrNullAsync

class WelcomeExtension : CabinetExtension("Welcome") {
    override suspend fun setup() {
        event<MemberJoinEvent> {
            check {
                val settings = cacheNonnullThenGet("welcomeSettings", "Guild has no settings.") {
                    newSuspendedTransaction {
                        GuildSetting.findBySnowflake(event.guildId)
                            .map { it.welcomeData }
                            .orNull()
                    }
                }

                if (!settings.enabled)
                    fail("Welcoming is disabled.")
                if (settings.ignoreBots && event.member.isBot)
                    fail("User is a bot and this guild disallows welcoming bots.")

                cacheNonnull("welcomeChannel", "Guild has no welcome channel.") {
                    tryOrNullAsync {
                        it.guild.getChannelOf<TextChannel>(settings.channel)
                    }.await()
                }
            }
            action {
                val welcome by cache<WelcomeData>("welcomeSettings")
                val channel by cache<TextChannel>("welcomeChannel")

                if (welcome.joinDmFormat.isNotEmpty())
                    event.member.createEmbedOrNull {
                        color = welcome.color
                        description = welcome.formatDmMessage(event.member, event.getGuild())
                    }
                if (welcome.joinFormat.isNotEmpty())
                    channel.createEmbed {
                        color = welcome.color
                        description = welcome.formatJoinMessage(event.member, event.getGuild())
                    }
            }
        }

        event<MemberLeaveEvent> {
            createCheck {
                val settings = cacheNonnullThenGet("welcomeSettings", "Guild has no settings.") {
                    newSuspendedTransaction {
                        GuildSetting.findBySnowflake(event.guildId)
                            .map { it.welcomeData }
                            .orNull()
                    }
                }

                if (!settings.enabled)
                    fail("Welcoming is disabled.")

                if (settings.ignoreBots && event.user.isBot)
                    fail("User is a bot and this guild disallows welcoming bots.")

                cacheNonnull("welcomeChannel", "Guild has no welcome channel.") {
                    tryOrNullAsync {
                        it.guild.getChannelOf<TextChannel>(settings.channel)
                    }.await()
                }
            }
            action {
                val welcome by cache<WelcomeData>("welcomeSettings")
                val channel by cache<TextChannel>("welcomeChannel")

                if (welcome.departFormat.isNotEmpty())
                    channel.createEmbed {
                        color = welcome.color
                        description = welcome.formatJoinMessage(event.user, event.getGuild())
                    }
            }
        }
    }
}*/