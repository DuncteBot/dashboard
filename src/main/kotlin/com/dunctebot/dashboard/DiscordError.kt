package com.dunctebot.dashboard

enum class DiscordError(val title: String, view: String) {
    NO_GUILD("", "noGuild"),
    NO_PERMS("You are missing some permissions!", "noPerms")
    ;

    val viewPath = "errors/discord/$view.vm"
}
