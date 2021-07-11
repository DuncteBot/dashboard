package com.dunctebot.dashboard

enum class DiscordError(val title: String, view: String) {
    NO_GUILD("Server not found", "noGuild"),
    NO_PERMS("You are missing some permissions!", "noPerms"),
    WAT("WAT", "wat"),
    ;

    val viewPath = "errors/discord/$view.vm"
}
