package com.dunctebot.dashboard.utils

import discord4j.common.util.Snowflake
import discord4j.core.`object`.PermissionOverwrite
import discord4j.core.util.PermissionUtil
import discord4j.discordjson.Id
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
import java.util.stream.Stream

fun RestGuild.getEveryoneRole(): Mono<RoleData> {
    return this.role(this.id).data
}

val Id.snowflake: Snowflake
    get() = Snowflake.of(this)

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

fun ChannelData.getPermissionOverwrites(): Set<PermissionOverwrite> {
    return this.permissionOverwrites().toOptional()
        .map { permissionOverwrites ->
            return@map permissionOverwrites.stream()
                .map { data ->
                    when (data.type()) {
                        "role" -> PermissionOverwrite.forRole(
                            data.id().snowflake,
                            PermissionSet.of(data.allow()),
                            PermissionSet.of(data.deny())
                        )
                        "member" -> PermissionOverwrite.forMember(
                            data.id().snowflake,
                            PermissionSet.of(data.allow()),
                            PermissionSet.of(data.deny())
                        )
                        else -> throw IllegalArgumentException("Unknown override type ${data.type()}")
                    }
                }
                .collect(Collectors.toSet())
        }
        .orElse(Collections.emptySet())
}

private fun getOverridesForRole(roleId: Snowflake, all: Set<PermissionOverwrite>): Optional<PermissionOverwrite> {
    return all.stream().filter { it.roleId.map(roleId::equals).orElse(false) }.findFirst()
}

private fun getOverridesForMember(memberId: Snowflake, all: Set<PermissionOverwrite>): Optional<PermissionOverwrite> {
    return all.stream().filter { it.memberId.map(memberId::equals).orElse(false) }.findFirst()
}

fun ChannelData.getEffectivePermissions(guild: RestGuild, member: MemberData): Mono<PermissionSet> {
    return member.getBasePermissions(guild).map { basePerms ->
        val permissionOverwrites = this.getPermissionOverwrites()
        val everyoneOverwrite = getOverridesForRole(guild.id, permissionOverwrites).orElse(null)

        val roleOverwrites = member.roles()
            .stream()
            .map { getOverridesForRole(it.snowflake, permissionOverwrites) }
            .flatMap { it.map { item -> Stream.of(item) }.orElseGet { Stream.empty() } }
            .toList()
        val memberOverwrites = getOverridesForMember(member.user().id().snowflake, permissionOverwrites).orElse(null)

        return@map PermissionUtil.computePermissions(basePerms, everyoneOverwrite, roleOverwrites, memberOverwrites)
    }
}
