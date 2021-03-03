package com.dunctebot.dashboard.utils

import com.dunctebot.dashboard.jsonMapper
import com.dunctebot.dashboard.webSocket
import com.fasterxml.jackson.databind.JsonNode
import java.util.concurrent.CompletableFuture

fun fetchGuildPatronStatus(guildId: String): Boolean {
    val future = CompletableFuture<JsonNode>()
    val json = jsonMapper.createObjectNode()

    json.putArray("guild_patron_status")
        .add(guildId)

    webSocket.requestData(json, future::complete)

    return future.get()["guild_patron_status"][guildId].asBoolean()
}
