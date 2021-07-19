package com.dunctebot.dashboard.websocket.handlers

import com.dunctebot.dashboard.discordClient
import com.dunctebot.dashboard.websocket.handlers.base.SocketHandler
import com.fasterxml.jackson.databind.JsonNode

class DataUpdateHandler : SocketHandler() {
    override fun handleInternally(data: JsonNode?) {
        if (data!!.has("guilds")) {
            data["guilds"]["invalidate"].forEach {
                discordClient.invalidateGuild(it.asLong())
            }
        }
    }
}
