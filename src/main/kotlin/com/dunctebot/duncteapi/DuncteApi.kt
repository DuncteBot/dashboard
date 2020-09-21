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

package com.dunctebot.duncteapi

import com.dunctebot.dashboard.constants.ContentType.JSON
import com.dunctebot.dashboard.httpClient
import com.dunctebot.dashboard.jsonMapper
import com.dunctebot.models.settings.GuildSetting
import com.dunctebot.models.settings.WarnAction
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.github.benmanes.caffeine.cache.Caffeine
import com.jagrosh.jdautilities.oauth2.entities.OAuth2Guild
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

class DuncteApi(private val apiKey: String) {
    private val logger = LoggerFactory.getLogger(DuncteApi::class.java)
    val validTokens = mutableListOf<String>()
    // caching makes it a bit faster
    private val settingCache = Caffeine.newBuilder()
        .expireAfterAccess(30, TimeUnit.MINUTES)
        .build<Long, GuildSetting>()

    fun validateToken(token: String): Boolean {
        if (validTokens.contains(token)) {
            return true
        }

        val json = executeRequest(defaultRequest("validate-token?bot_routes=true&the_token=$token"))
        val isValid = json["success"].asBoolean()

        // cache the valid tokens to make validation faster
        if (isValid) {
            validTokens.add(token)
        }

        return isValid
    }

    fun isOneGuildPatron(userId: String): Boolean {
        val json = executeRequest(defaultRequest("patrons/oneguild/$userId"))

        return !json["data"].isEmpty
    }

    fun getGuildSetting(guildId: Long): GuildSetting {
        return settingCache.get(guildId) {
            val json = executeRequest(defaultRequest("guildsettings/$guildId"))

            return@get jsonMapper.readValue(json["data"].traverse(), GuildSetting::class.java)
        }!!
    }

    fun saveGuildSetting(setting: GuildSetting) {
        val json = setting.toJson(jsonMapper)

        patchJSON("guildsettings/${setting.guildId}", json)
    }

    fun updateWarnActions(guildId: Long, actions: List<WarnAction>) {
        val json = jsonMapper.createObjectNode()

        json.putArray("warn_actions")
            .addAll(jsonMapper.valueToTree<ArrayNode>(actions))

        val response = postJSON("guildsettings/$guildId/warn-actions", json)

        if (!response["success"].asBoolean()) {
            logger.error("Failed to set warn actions for $guildId\n" +
                "Response: {}", response["error"].toString())
        }
    }

    fun fetchCustomCommands(guildId: Long): JsonNode {
        return executeRequest(defaultRequest("customcommands/$guildId"))["data"]
    }

    fun fetchCustomCommand(guildId: Long, invoke: String): JsonNode? {
        val resp = executeRequest(defaultRequest("customcommands/$guildId/$invoke"))

        if (!resp["success"].asBoolean()) {
            return null
        }

        return resp["data"]
    }

    fun updateCustomCommand(guildId: Long, command: JsonNode): Triple<Boolean, Boolean, Boolean> {
        val response = patchJSON("customcommands/$guildId/${command["name"].asText()}", command)

        return parseTripleResponse(response)
    }

    private fun patchJSON(path: String, json: JsonNode, prefixBot: Boolean = true): JsonNode {
        val body = RequestBody.create(null, json.toJsonString())
        val request = defaultRequest(path, prefixBot)
            .patch(body).addHeader("Content-Type", JSON)

        return executeRequest(request)
    }

    private fun postJSON(path: String, json: JsonNode, prefixBot: Boolean = true): JsonNode {
        val body = RequestBody.create(null, json.toJsonString())
        val request = defaultRequest(path, prefixBot)
            .post(body).addHeader("Content-Type", JSON)

        return executeRequest(request)
    }

    private fun deleteJSON(path: String, json: JsonNode, prefixBot: Boolean = true): JsonNode {
        val body = RequestBody.create(null, json.toJsonString())
        val request = defaultRequest(path, prefixBot)
            .delete(body).addHeader("Content-Type", JSON)

        return executeRequest(request)
    }

    private fun defaultRequest(path: String, prefixBot: Boolean = true): Request.Builder {
        val prefix = if (prefixBot) "bot/" else ""

        return Request.Builder()
            .url("$API_HOST/$prefix$path")
            .get()
            .addHeader("Authorization", apiKey)
    }

    private fun executeRequest(request: Request.Builder): JsonNode {
        httpClient.newCall(request.build())
            .execute().use {
                return jsonMapper.readTree(it.body()!!.byteStream())
            }
    }

    private fun parseTripleResponse(response: JsonNode): Triple<Boolean, Boolean, Boolean> {
        val success = response["success"].asBoolean()

        if (success) {
            return Triple(first = true, second = false, third = false)
        }

        val error = response["error"]
        val type = error["type"].asText()

        if (type == "AmountException") {
            return Triple(first = false, second = false, third = true)
        }

        if (type !== "ValidationException") {
            return Triple(first = false, second = false, third = false)
        }

        val errors = response["error"]["errors"]

        for (key in errors.fieldNames()) {
            errors[key].forEach { reason ->
                if (reason.asText().contains("The invoke has already been taken.")) {
                    return Triple(first = false, second = true, third = false)
                }
            }
        }

        return Triple(first = false, second = false, third = false)
    }

    private fun JsonNode.toJsonString() = jsonMapper.writeValueAsString(this)

    companion object {
        // const val API_HOST = "http://localhost:8081"
        const val API_HOST = "http://duncte123-apis-lumen.test/"
        // const val API_HOST = "https://apis.duncte123.me"
        const val USER_AGENT = "Mozilla/5.0 (compatible; SkyBot/dashboard; +https://dashboard.dunctebot.com)"
    }
}
