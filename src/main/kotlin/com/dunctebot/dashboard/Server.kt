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

import com.dunctebot.dashboard.controllers.GuildController
import com.dunctebot.dashboard.controllers.RootController
import com.dunctebot.dashboard.controllers.api.OtherAPi
import com.dunctebot.dashboard.controllers.errors.HttpErrorHandlers
import com.dunctebot.dashboard.rendering.VelocityRenderer
import com.dunctebot.dashboard.rendering.WebVariables
import com.dunctebot.dashboard.websocket.EchoWebSocket
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.json.JsonMapper
import com.jagrosh.jdautilities.oauth2.OAuth2Client
import io.github.cdimascio.dotenv.Dotenv
import spark.ModelAndView
import spark.Spark.*

// TODO: add socket server http://sparkjava.com/documentation#websockets
// The socket server will be used to communicate with DuncteBot himself
// NGINX can secure the websocket (hopefully it does this by default as we are using the same domain)
class Server(private val env: Dotenv) {
    private val mapper = JsonMapper()
    private val engine = VelocityRenderer(env)
    private val oAuth2Client = OAuth2Client.Builder()
        .setClientId(env["OAUTH_CLIENT_ID"]!!.toLong())
        .setClientSecret(env["OAUTH_CLIENT_SECRET"]!!)
        /*.setOkHttpClient(
            OkHttpClient.Builder()
                // hack until JDA-Utils fixes the domain
                .addInterceptor {
                    var request = it.request()
                    val httpUrl = request.url()

                    if (httpUrl.host().contains("discordapp")){
                        val newHttpUrl = httpUrl.newBuilder()
                            .host("discord.com")
                            .build()

                        request = request.newBuilder()
                            .url(newHttpUrl)
                            .build()
                    }

                    it.proceed(request)
                }
                .build()
        )*/
        .build()

    init {
        port(env["SERVER_PORT"]!!.toInt())
        ipAddress(env["SERVER_IP"])

        if (env["IS_LOCAL"]!!.toBoolean()) {
            val projectDir = System.getProperty("user.dir")
            val staticDir = "/src/main/resources/public"
            staticFiles.externalLocation(projectDir + staticDir)
        } else {
            staticFiles.location("/public")
        }

        val responseTransformer: (Any) -> String = {
            when (it) {
                is JsonNode -> {
                    mapper.writeValueAsString(it)
                }
                is ModelAndView -> {
                    engine.render(it)
                }
                else -> {
                    it.toString()
                }
            }
        }

        defaultResponseTransformer(responseTransformer)

        webSocket("/echo", EchoWebSocket::class.java)

        // Non settings related routes
        get("/roles/:hash") { request, response ->
            return@get GuildController.showGuildRoles(request, response)
        }

        addDashboardRoutes()
        addAPIRoutes()

        notFound { request, response ->
            val result = HttpErrorHandlers.notFound(request, response, mapper)

            return@notFound responseTransformer(result)
        }

        internalServerError { request, response ->
            val result = HttpErrorHandlers.internalServerError(request, response, mapper)

            return@internalServerError responseTransformer(result)
        }

        // I hate how they made it varargs
        // now I have to add parentheses
        after({ _, response ->
            // enable gzip, this is done automagically by sending the header
            response.header("Content-Encoding", "gzip")
        })
    }

    private fun addDashboardRoutes() {
        get("/callback") { request, response ->
            return@get RootController.callback(request, response, oAuth2Client)
        }

        get("/logout") { request, response ->
            request.session().invalidate()

            return@get response.redirect(HOMEPAGE)
        }

        path("/") {
            before("") { request, response ->
                return@before RootController.beforeRoot(request, response, oAuth2Client, env)
            }

            get("") { _, _ ->
                return@get WebVariables()
                    .put("title", "Dashboard")
                    .toModelAndView("dashboard/index.vm")
            }
        }
    }

    private fun addAPIRoutes() {
        path("/api") {
            before("/*") { _, response ->
                response.type("application/json")
                response.header("Access-Control-Allow-Origin", "*")
                response.header("Access-Control-Allow-Credentials", "true")
                response.header("Access-Control-Allow-Methods", "POST, GET, OPTIONS, DELETE, PATCH")
                response.header("Access-Control-Allow-Headers", "Content-Type, Accept, X-Requested-With, Authorization")
                response.header("Access-Control-Max-Age", "3600")
            }

            options("/*") { _, _ ->
                // Allow OPTIONS requests
            }

            get("/user-guilds") { request, response ->
                return@get OtherAPi.getUserGuilds(request, response, oAuth2Client, mapper)
            }

            get("/commands.json") {_, _ ->
                "TODO: setup websocket to bot"
            }
        }
    }

    fun shutdown() {
        awaitStop()
    }

    companion object {
        const val FLASH_MESSAGE = "FLASH_MESSAGE"
        const val OLD_PAGE = "OLD_PAGE"
        const val SESSION_ID = "sessionId"
        const val USER_ID = "USER_SESSION"
        const val GUILD_ID = ":guildid"
        const val HOMEPAGE = "https://dunctebot.com/"
    }
}
