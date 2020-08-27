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

package com.dunctebot.dashboard

import com.dunctebot.dashboard.Server.Companion.GUILD_ID
import com.dunctebot.dashboard.Server.Companion.SESSION_ID
import com.dunctebot.dashboard.Server.Companion.USER_ID
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.jagrosh.jdautilities.oauth2.OAuth2Client
import com.jagrosh.jdautilities.oauth2.session.Session
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.sharding.ShardManager
import net.dv8tion.jda.internal.utils.IOUtil
import okhttp3.FormBody
import okhttp3.OkHttpClient
import spark.*
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

private fun String.decodeUrl() = URLDecoder.decode(this, StandardCharsets.UTF_8);

// TODO: Might just send a json body instead
val Request.paramsMap: Map<String, String>
    get () {
        val body = this.body()
        val map = hashMapOf<String, String>()

        body.split("&").forEach {
            val split = it.split("=")

            map[split[0].decodeUrl()] = split[1].decodeUrl()
        }

        return map
    }

val Request.userId: String
    get() = this.session().attribute(USER_ID) as String

fun Request.getGuild(shardManager: ShardManager): Guild? = shardManager.getGuildById(this.params(GUILD_ID))

fun Request.getSession(oAuth2Client: OAuth2Client): Session? {
    val session: String? = this.session().attribute(SESSION_ID)

    if (session.isNullOrEmpty()) {
        return null
    }

    return oAuth2Client.sessionController.getSession(session)
}

fun String?.toCBBool(): Boolean = if (this.isNullOrEmpty()) false else (this == "on")

object WebHelpers {

    fun haltNotFound(request: Request, response: Response) {
        Spark.halt(404, CustomErrorPages.getFor(404, request, response) as String)
    }

    fun verifyCaptcha(httpClient: OkHttpClient, response: String, secret: String, mapper: ObjectMapper): JsonNode {
        val body = FormBody.Builder()
            .add("secret", secret)
            .add("response", response)
            .build()

        httpClient.newCall(
            okhttp3.Request.Builder()
                .url("https://hcaptcha.com/siteverify")
                .patch(body)
                .build()
        ).execute().use {
            val readFully = IOUtil.readFully(IOUtil.getBody(it))

            return mapper.readTree(readFully)
        }
    }
}
