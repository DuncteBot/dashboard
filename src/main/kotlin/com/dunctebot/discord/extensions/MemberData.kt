package com.dunctebot.discord.extensions

import com.dunctebot.dashboard.discordClient
import com.dunctebot.discord.isPermissionApplied
import discord4j.discordjson.json.MemberData
import discord4j.discordjson.json.RoleData
import discord4j.rest.util.Permission
import discord4j.rest.util.PermissionSet
import reactor.core.publisher.Flux

fun MemberData.hasPermission(guildId: Long, perm: Permission): Boolean {
    val permissions = this.fetchPermissions(guildId)

    return permissions.contains(Permission.ADMINISTRATOR) || permissions.contains(perm)
}

// TODO: permission cache
fun MemberData.fetchPermissions(guildId: Long): PermissionSet {
    val roles = this.roles()

    if (roles.isEmpty()) {
        return PermissionSet.none()
    }

    var perms = PermissionSet.none()

    roles.forEach {
        val rolePerms = discordClient.retrieveRole(guildId, it.asLong())
            .block()!!
            .permissions()

        // If the permission is not applied yet
        perms = perms.or(PermissionSet.of(rolePerms))
    }

    return perms
}

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
