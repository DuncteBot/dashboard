package com.dunctebot.discord.extensions

import com.dunctebot.discord.isPermissionApplied
import discord4j.discordjson.json.MemberData
import discord4j.discordjson.json.RoleData
import discord4j.rest.util.Permission
import reactor.core.publisher.Flux

fun MemberData.hasPermission(guildRoles: Flux<RoleData>, perm: Permission): Boolean {
    return isPermissionApplied(this.getPermissionsRaw(guildRoles), perm.value)
}

fun MemberData.getPermissionsRaw(guildRoles: Flux<RoleData>): Long {
    val roles = this.roles()

    if (roles.isEmpty()) {
        return 0L
    }

    var perms = 0L

    guildRoles.filter { roles.contains(it.id()) }
        .map { it.permissions() }
        .collectList().block()!!
        .forEach {
            // If the permission is not applied yet
            if (!isPermissionApplied(perms, it)) {
                perms = perms or it
            }
        }

    return perms
}
