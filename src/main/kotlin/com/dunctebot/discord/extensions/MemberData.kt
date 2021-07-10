package com.dunctebot.discord.extensions

import discord4j.discordjson.json.MemberData
import discord4j.discordjson.json.RoleData
import discord4j.rest.util.Permission
import reactor.core.publisher.Flux

fun MemberData.getPermissions(guildRoles: Flux<RoleData>): Set<Permission> {
    val roles = this.roles()

    if (roles.isEmpty()) {
        return setOf()
    }

    val perms = mutableSetOf<Permission>()

    val memberRoles = guildRoles.filter { roles.contains(it.id()) }
        .map { it.permissions() }
        .collectList().block()!!

    Permission.values().forEach {
        if (memberRoles.contains(it.value)) {
            perms.add(it)
        }
    }

    return perms
}
