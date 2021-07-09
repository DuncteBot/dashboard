package com.dunctebot.discord

import com.github.benmanes.caffeine.cache.Caffeine
import discord4j.common.util.Snowflake
import discord4j.core.DiscordClient
import discord4j.discordjson.json.*
import discord4j.rest.entity.RestGuild
import discord4j.rest.entity.RestUser
import net.dv8tion.jda.api.entities.Guild
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.concurrent.TimeUnit


/**
 * Custom jda rest client that allows for rest-only usage of JDA
 *
 * This class has been inspired by GivawayBot and all credit goes to them https://github.com/jagrosh/GiveawayBot
 */
class DiscordRestClient(token: String) {
    // create a guild cache that keeps the guilds in cache for 30 minutes
    // When we stop accessing the guild it will be removed from the cache
    // TODO: use expiring map instead
    private val guildCache = Caffeine.newBuilder()
        .expireAfterAccess(30, TimeUnit.MINUTES)
        .build<String, Guild>()

    private val client = DiscordClient.create(token)

    // TODO: still needed?
    fun invalidateGuild(guildId: Long) {
        this.guildCache.invalidate(guildId.toString())
    }

    // Note: instantly starts the request
    fun retrieveD4JUserById(id: String): RestUser {
        return this.client.getUserById(Snowflake.of(id))
    }

    // TODO: cache this
    fun retrieveD4JSelfUser(): Mono<UserData> {
        return this.client.self
    }

    // TODO: cache this
    fun retrieveGuildData(guildId: Long): GuildUpdateData {
        return this.getGuild(guildId).data.block()!!
    }

    fun getGuild(guildId: Long): RestGuild {
        return this.client.getGuildById(Snowflake.of(guildId))
    }

    fun retrieveGuildRoles(guildId: Long): Flux<RoleData> {
        return this.getGuild(guildId).roles
    }

    fun retrieveGuildMembers(guildId: Long): Flux<MemberData> {
        return this.getGuild(guildId).members
    }

    fun retrieveMemberById(guildId: Long, memberId: String): Mono<MemberData> {
        return this.getGuild(guildId).getMember(Snowflake.of(memberId))
    }
}
