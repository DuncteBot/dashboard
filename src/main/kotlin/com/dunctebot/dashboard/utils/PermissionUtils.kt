package com.dunctebot.dashboard.utils

import discord4j.common.util.Snowflake
import discord4j.core.`object`.ExtendedPermissionOverwrite
import discord4j.core.util.PermissionUtil
import discord4j.discordjson.json.ChannelData
import discord4j.discordjson.json.MemberData
import discord4j.discordjson.json.RoleData
import discord4j.rest.entity.RestGuild
import discord4j.rest.util.PermissionSet
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.*
import java.util.function.Predicate
import java.util.stream.Collectors

fun RestGuild.getEveryoneRole(): Mono<RoleData> {
    return this.role(this.id).data
}

/**
 * Adapted from https://github.com/Discord4J/Discord4J/blob/0e29720384ce5da312ab81994ac89c9607e75d96/core/src/main/java/discord4j/core/object/entity/Member.java#L442
 */
fun MemberData.getBasePermissions(guild: RestGuild): Mono<PermissionSet> {
    val getIsOwner = guild.data.map { it.ownerId() == this.user().id() }
    val getEveryonePerms = guild.getEveryoneRole().map { PermissionSet.of(it.permissions()) }
    val getRolePerms = Flux.fromIterable(this.roles())
        .flatMap { guild.role(Snowflake.of(it)).data }
        .map { PermissionSet.of(it.permissions()) }
        .collectList()

    return getIsOwner.filter(Predicate.isEqual(true))
        .flatMap { Mono.just(PermissionSet.all()) }
        .switchIfEmpty(Mono.zip(getEveryonePerms, getRolePerms, PermissionUtil::computeBasePermissions))
}

fun ChannelData.getPermissionOverwrites(): Set<ExtendedPermissionOverwrite> {
    return this.permissionOverwrites().toOptional()
        .map { permissionOverwrites ->
            val guildId = guildId().get().asLong()
            val channelId = id().asLong()

            return@map permissionOverwrites.stream()
                .map { data ->
                    // TODO: how to turn this into rest
                    ExtendedPermissionOverwrite(getClient(), data, guildId, channelId)
                }
                .collect(Collectors.toSet())
        }
        .orElse(Collections.emptySet())
}

fun ChannelData.getEffectivePermissions(guild: RestGuild, member: MemberData): Mono<PermissionSet> {
    return member.getBasePermissions(guild).map { basePerms ->

        this.permissionOverwrites().toOptional()

        return@map PermissionUtil.computePermissions(basePerms, null, listOf(), null)
    }
}
