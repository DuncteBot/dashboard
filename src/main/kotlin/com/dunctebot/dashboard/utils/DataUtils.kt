package com.dunctebot.dashboard.utils

import com.dunctebot.dashboard.jsonMapper
import com.dunctebot.dashboard.webSocket
import com.dunctebot.models.settings.GuildSetting
import com.fasterxml.jackson.databind.JsonNode
import java.util.concurrent.CompletableFuture

fun fetchGuildData(guildId: String): Pair<GuildSetting, Boolean> {
    val future = CompletableFuture<JsonNode>()
    val json = jsonMapper.createObjectNode()

    json.putArray("guild_settings")
        .add(guildId)
    json.putArray("guild_patron_status")
        .add(guildId)

    webSocket.requestData(json, future::complete)

    val result = future.get()

    println(result)

    val guildSetting = result["guild_settings"][guildId]
    val settingParsed = jsonMapper.readValue(guildSetting.traverse(), GuildSetting::class.java)
    val patreonStatus = result["guild_patron_status"][guildId].asBoolean()

    return settingParsed to patreonStatus
}
