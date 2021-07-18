package com.dunctebot.dashboard.utils

import discord4j.common.util.Snowflake
import discord4j.core.util.PermissionUtil
import discord4j.discordjson.json.MemberData
import discord4j.discordjson.json.RoleData
import discord4j.rest.entity.RestGuild
import discord4j.rest.util.PermissionSet
import reactor.core.publisher.Mono
import java.util.function.Predicate

fun RestGuild.getEveryoneRole(): Mono<RoleData> {
    return this.role(this.id).data
}

/**
 * Adapted from https://github.com/Discord4J/Discord4J/blob/0e29720384ce5da312ab81994ac89c9607e75d96/core/src/main/java/discord4j/core/object/entity/Member.java#L442
 */
fun MemberData.getBasePermissions(guild: RestGuild): Mono<PermissionSet> {
    val getIsOwner = guild.data.map { it.ownerId() == this.user().id() }
    val getEveryonePerms = guild.getEveryoneRole().map { PermissionSet.of(it.permissions()) }
    val getRolePerms = Mono.create<List<PermissionSet>> {
        this.roles().map { guild.role(Snowflake.of(it)).data }
            .map {
                it.flatMap { r -> Mono.from<PermissionSet> { PermissionSet.of(r.permissions()) } }
            }
            .flatMap { it.block()!! }
    }

    return getIsOwner.filter(Predicate.isEqual(true))
        .flatMap { ignored -> Mono.just(PermissionSet.all()) }
        .switchIfEmpty(Mono.zip(getEveryonePerms, getRolePerms, PermissionUtil::computeBasePermissions))
}
