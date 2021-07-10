package com.dunctebot.discord

fun isPermissionApplied(totalPermissions: Long, permToCheck: Long): Boolean {
    return (totalPermissions and permToCheck) == permToCheck
}
