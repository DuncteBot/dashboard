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

import com.dunctebot.dashboard.Server.Companion.SESSION_ID
import com.dunctebot.dashboard.Server.Companion.USER_ID
import com.dunctebot.dashboard.getSession
import com.dunctebot.dashboard.jda
import com.dunctebot.dashboard.userId
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.github.benmanes.caffeine.cache.Caffeine
import com.jagrosh.jdautilities.oauth2.OAuth2Client
import com.jagrosh.jdautilities.oauth2.entities.OAuth2Guild
import net.dv8tion.jda.api.Permission
import spark.Request
import spark.Response
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.TimeUnit

object OtherAPi {
    val guildsRequests = Caffeine.newBuilder()
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .build<String, List<OAuth2Guild>>()

    fun getUserGuilds(request: Request, response: Response, oAuth2Client: OAuth2Client, mapper: JsonMapper): Any {
        val attributes = request.session().attributes()

        // We need to make sure that we are logged in and have a user id
        // If we don't have either of them we will return an error message
        if (!attributes.contains(USER_ID) || !attributes.contains(SESSION_ID)) {
            request.session().invalidate()

            return mapper.createObjectNode()
                .put("status", "error")
                .put("message", "SESSION_INVALID")
                .put("code", response.status())
        }

        val guilds = mapper.createArrayNode()

        // Attempt to get all guilds from the cache
        // We are using a cache here to make sure we don't rate limit our application
        val guildsRequest = guildsRequests.get(request.userId) {
            return@get oAuth2Client.getGuilds(request.getSession(oAuth2Client)).complete()
        }!!

        guildsRequest.forEach {
            // Only add the servers where the user has the MANAGE_SERVER
            // perms to the list
            if (it.hasPermission(Permission.MANAGE_SERVER)) {
                guilds.add(guildToJson(it, mapper))
            }
        }

        return mapper.createObjectNode()
            .put("status", "success")
            .put("total", guildsRequest.size)
            .put("code", response.status())
            .set<ObjectNode>("guilds", guilds)
    }

    private fun guildToJson(guild: OAuth2Guild, mapper: JsonMapper): JsonNode {
        // Get guild id or random default avatar url
        val icon = if (!guild.iconUrl.isNullOrEmpty()) {
            guild.iconUrl
        } else {
            val number = ThreadLocalRandom.current().nextInt(0, 5)
            "https://cdn.discordapp.com/embed/avatars/$number.png"
        }

        val memberCount = jda.fakeJDA.getGuildById(guild.idLong)?.memberCount ?: -1

        return mapper.createObjectNode()
            .put("name", guild.name)
            .put("iconId", guild.iconId)
            .put("iconUrl", icon)
            .put("owner", guild.isOwner)
            .put("members", memberCount)
            .put("id", guild.id)
    }
}
