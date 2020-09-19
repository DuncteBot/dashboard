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
import com.fasterxml.jackson.databind.JsonNode
import okhttp3.Request
import okhttp3.RequestBody

class DuncteApi(private val apiKey: String) {
    val validTokens = mutableListOf<String>()

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
        val json = executeRequest(defaultRequest("guildsettings/$guildId"))

        return jsonMapper.readValue(json["data"].traverse(), GuildSetting::class.java)
    }

    fun saveGuildSetting(setting: GuildSetting) {
        val json = setting.toJson(jsonMapper)


        patchJSON("guildsettings/${setting.guildId}", json)
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

    private fun JsonNode.toJsonString() = jsonMapper.writeValueAsString(this)

    companion object {
        // const val API_HOST = "http://localhost:8081"
        const val API_HOST = "http://duncte123-apis-lumen.test/"
        // const val API_HOST = "https://apis.duncte123.me"
        const val USER_AGENT = "Mozilla/5.0 (compatible; SkyBot/dashboard; +https://dashboard.dunctebot.com)"
    }
}
