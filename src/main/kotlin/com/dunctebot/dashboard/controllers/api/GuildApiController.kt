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
import com.dunctebot.dashboard.controllers.GuildController
import com.dunctebot.dashboard.utils.HashUtils
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import net.dv8tion.jda.api.entities.User
import spark.Request
import spark.Response
import java.util.concurrent.CompletableFuture

object GuildApiController {
    fun findUserAndGuild(request: Request, response: Response): Any {
        val data = request.jsonBody

        if (!(data.has("user_id") && data.has("guild_id") && data.has("captcha_response"))) {
            response.status(406)

            return jsonMapper.createObjectNode()
                .put("success", false)
                .put("message", "missing_input")
                .put("code", response.status())
        }

        if (data["captcha_response"].isNull) {
            response.status(406)

            return jsonMapper.createObjectNode()
                .put("success", false)
                .put("message", "Captcha missing")
                .put("code", response.status())

        }

        val captchaResponse = data["captcha_response"].asText()
        val captchaResult = verifyCaptcha(captchaResponse)

        if (!captchaResult["success"].asBoolean()) {
            response.status(403)

            return jsonMapper.createObjectNode()
                .put("success", false)
                .put("message", "Could not validate that you are a human")
                .put("code", response.status())
        }

        val user: User? = try {
            restJDA.retrieveUserById(data["user_id"].asText()).complete()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

        if (user == null) {
            response.status(404)

            return jsonMapper.createObjectNode()
                .put("success", false)
                .put("message", "no_user")
                .put("code", response.status())
        }

        val guild: JsonNode? = try {
            val future = CompletableFuture<JsonNode>()

            val json = jsonMapper.createObjectNode()
                json.putArray("partial_guilds")
                .add(data["guild_id"].asText())
            webSocket.requestData(json, future::complete)

            val partialGuilds = future.get()["partial_guilds"]

            if (partialGuilds.isEmpty) {
                null
            } else {
                val respJson = partialGuilds[0]

                if (respJson["member_count"].asInt() == -1) {
                    null
                } else {
                    respJson
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

        if (guild == null) {
            response.status(404)

            return jsonMapper.createObjectNode()
                .put("success", false)
                .put("message", "no_guild")
                .put("code", response.status())
        }

        val guildId = guild["id"].asText()
        val guildJson = jsonMapper.createObjectNode()
            .put("id", guildId)
            .put("name", guild["name"].asText())

        val userJson = jsonMapper.createObjectNode()
            .put("id", user.id)
            .put("name", user.name)
            .put("formatted", user.asTag)

        val theKey = "${user.idLong}-${guildId}"
        val theHash = HashUtils.sha1(theKey + System.currentTimeMillis())

        GuildController.securityKeys[theHash] = theKey

        val node = jsonMapper.createObjectNode()
            .put("success", true)
            .put("token", theHash)
            .put("code", response.status())

        node.set<ObjectNode>("user", userJson)
        node.set<ObjectNode>("guild", guildJson)

        return node
    }
}
