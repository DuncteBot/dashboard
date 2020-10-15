/*
 * MIT License
 *
 * Copyright (c) 2020 Duncan Sterken
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.dunctebot.dashboard.controllers.api

import com.dunctebot.dashboard.*
import com.dunctebot.dashboard.WebServer.Companion.SESSION_ID
import com.dunctebot.dashboard.WebServer.Companion.USER_ID
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.github.benmanes.caffeine.cache.Caffeine
import com.jagrosh.jdautilities.oauth2.OAuth2Client
import com.jagrosh.jdautilities.oauth2.entities.OAuth2Guild
import net.dv8tion.jda.api.Permission
import spark.Request
import spark.Response
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.TimeUnit

object OtherAPi {
    val guildsRequests = Caffeine.newBuilder()
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .build<String, List<OAuth2Guild>>()

    fun fetchGuildsOfUser(request: Request, response: Response, oAuth2Client: OAuth2Client): Any {
        val attributes = request.session().attributes()

        // We need to make sure that we are logged in and have a user id
        // If we don't have either of them we will return an error message
        if (!attributes.contains(USER_ID) || !attributes.contains(SESSION_ID)) {
            request.session().invalidate()

            return jsonMapper.createObjectNode()
                .put("success", false)
                .put("message", "SESSION_INVALID")
                .put("code", response.status())
        }

        val guilds = jsonMapper.createArrayNode()

        // Attempt to get all guilds from the cache
        // We are using a cache here to make sure we don't rate limit our application
        val guildsRequest = guildsRequests.get(request.userId) {
            return@get oAuth2Client.getGuilds(request.getSession(oAuth2Client)).complete()
        }!!

        // Only add the servers where the user has the MANAGE_SERVER
        // perms to the list
        val filteredGilds = guildsRequest.filter { it.hasPermission(Permission.MANAGE_SERVER) }

        val future = CompletableFuture<JsonNode>()
        val json = jsonMapper.createObjectNode()
        val partialRequest = json.putArray("partial_guilds")

        // add all guild ids to the map
        filteredGilds.map(OAuth2Guild::getId).forEach(partialRequest::add)

        webSocket.requestData(json, future::complete)

        val partialGuilds = future.get()["partial_guilds"]

        filteredGilds.forEach { guilds.add(guildToJson(it, partialGuilds)) }

        return jsonMapper.createObjectNode()
            .put("success", true)
            .put("total", guildsRequest.size)
            .put("code", response.status())
            .set<ObjectNode>("guilds", guilds)
    }

    private fun guildToJson(guild: OAuth2Guild, partialGuilds: JsonNode): JsonNode {
        // Get guild id or random default avatar url
        val icon = if (!guild.iconUrl.isNullOrEmpty()) {
            guild.iconUrl
        } else {
            val number = ThreadLocalRandom.current().nextInt(0, 5)
            "https://cdn.discordapp.com/embed/avatars/$number.png"
        }

        // #getId does a Long.toUnsignedString operation so we store it here to save some cycles
        val textId = guild.id
        val part = partialGuilds.find { it["id"].asText() == textId } ?: jsonMapper.createObjectNode().put("member_count", -1)

        return jsonMapper.createObjectNode()
            .put("name", guild.name)
            .put("iconId", guild.iconId)
            .put("iconUrl", icon)
            .put("owner", guild.isOwner)
            .put("members", part["member_count"].asInt())
            .put("id", textId)
    }

    fun uptimeRobot(): Any {
        return "This is just used by uptime robot to check if the application is up"
    }
}
