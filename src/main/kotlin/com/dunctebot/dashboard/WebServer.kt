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

import com.dunctebot.dashboard.controllers.DashboardController
import com.dunctebot.dashboard.controllers.GuildController
import com.dunctebot.dashboard.controllers.RootController
import com.dunctebot.dashboard.controllers.SettingsController
import com.dunctebot.dashboard.controllers.api.CustomCommandController
import com.dunctebot.dashboard.controllers.api.DataController
import com.dunctebot.dashboard.controllers.api.GuildApiController
import com.dunctebot.dashboard.controllers.api.OtherAPi
import com.dunctebot.dashboard.controllers.errors.HttpErrorHandlers
import com.dunctebot.dashboard.rendering.VelocityRenderer
import com.dunctebot.dashboard.rendering.WebVariables
import com.dunctebot.dashboard.utils.fetchGuildPatronStatus
import com.dunctebot.models.settings.GuildSetting
import com.dunctebot.models.settings.ProfanityFilterType
import com.dunctebot.models.settings.WarnAction
import com.dunctebot.models.utils.Utils
import com.fasterxml.jackson.databind.JsonNode
import com.jagrosh.jdautilities.oauth2.OAuth2Client
import io.github.cdimascio.dotenv.Dotenv
import net.dv8tion.jda.api.entities.TextChannel
import spark.ModelAndView
import spark.Spark.*

// The socket server will be used to communicate with DuncteBot himself
// NGINX can secure the websocket (hopefully it does this by default as we are using the same domain)
class WebServer(private val env: Dotenv) {
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
                    jsonMapper.writeValueAsString(it)
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

        // Non settings related routes
        get("/roles/:hash") { request, response ->
            return@get GuildController.showGuildRoles(request, response)
        }

        get("/register-server") { _, _ ->
            WebVariables()
                .put("hide_settings", true)
                .put("title", "Register your server for patron perks")
                .put("captcha_sitekey", env["CAPTCHA_SITEKEY"]!!)
                .toModelAndView("oneGuildRegister.vm")
        }

        post("/register-server") { request, _ ->
            return@post GuildController.handleOneGuildRegister(request)
        }

        addDashboardRoutes()
        addAPIRoutes()

        notFound { request, response ->
            val result = HttpErrorHandlers.notFound(request, response)

            return@notFound responseTransformer(result)
        }

        internalServerError { request, response ->
            val result = HttpErrorHandlers.internalServerError(request, response)

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
                    .put("hide_settings", true)
                    .toModelAndView("dashboard/index.vm")
            }
        }

        path("/server/$GUILD_ID") {
            before("*") { request, response ->
                return@before DashboardController.before(request, response)
            }

            get("") { request, response ->
                return@get response.redirect("/server/${request.guildId}/basic")
            }

            // basic settings
            getWithGuildData(
                "/basic",
                WebVariables().put("title", "Dashboard"),
                "dashboard/basicSettings.vm"
            )

            post("/basic") { request, response ->
                return@post SettingsController.saveBasic(request, response)
            }

            // Moderation settings
            getWithGuildData(
                "/moderation",
                WebVariables()
                    .put("filterValues", ProfanityFilterType.values())
                    .put("warnActionTypes", WarnAction.Type.values())
                    .put("title", "Dashboard")
                    .put("loggingTypes", GuildSetting.LOGGING_TYPES)
                    .put("patronMaxWarnActions", WarnAction.PATRON_MAX_ACTIONS),
                "dashboard/moderationSettings.vm"
            )

            post("/moderation") { request, response ->
                return@post SettingsController.saveModeration(request, response)
            }

            // Custom command settings
            getWithGuildData(
                "/custom-commands",
                WebVariables().put("title", "Dashboard"),
                "dashboard/customCommandSettings.vm"
            )

            // Message settings
            getWithGuildData(
                "/messages",
                WebVariables().put("title", "Dashboard"),
                "dashboard/welcomeLeaveDesc.vm"
            )

            post("/messages") { request, response ->
                return@post SettingsController.saveMessages(request, response)
            }


            // Soon tm?
            /*get("/music") { request, _ ->
                val guild = WebHelpers.getGuildFromRequest(request, shardManager)
                    ?: return@get """{"message": "No guild? WOT"}"""
                val mng = variables.audioUtils.getMusicManager(guild)

                EarthUtils.gMMtoJSON(mng, variables.jackson)
            }*/        }
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
                return@get OtherAPi.fetchGuildsOfUser(request, response, oAuth2Client)
            }

            // keep?
            get("/commands.json") { _, _ ->
                "TODO: setup websocket to bot"
            }

            post("/update-data") { request, _ ->
                return@post DataController.updateData(request)
            }

            get("/invalidate-tokens") { request, _ ->
                return@get DataController.invalidateTokens(request)
            }

            path("/check") {
                post("/user-guild") { request, response ->
                    return@post GuildApiController.findUserAndGuild(request, response)
                }
            }

            path("/custom-commands/$GUILD_ID") {
                before("") { request, response ->
                    return@before CustomCommandController.before(request, response)
                }

                get("") { request, response ->
                    return@get CustomCommandController.show(request, response)
                }

                patch("") { request, response ->
                    return@patch CustomCommandController.update(request, response)
                }

                post("") { request, response ->
                    return@post CustomCommandController.create(request, response)
                }

                delete("") { request, response ->
                    return@delete CustomCommandController.delete(request, response)
                }
            }
        }
    }

    fun shutdown() {
        awaitStop()
    }

    private fun getWithGuildData(path: String, map: WebVariables, view: String) {
        get(path) { request, _ ->
            val guild = request.fetchGuild()

            if (guild != null) {
                val guildId = guild.idLong

                val tcs = guild.textChannelCache.filter(TextChannel::canTalk).toList()
                val goodRoles = guild.roleCache.filter {
                    guild.selfMember.canInteract(it) && it.name != "@everyone" && it.name != "@here"
                }.filter { !it.isManaged }.toList()

                map.put("goodChannels", tcs)
                map.put("goodRoles", goodRoles)
                map.put("guild", guild)

                val settings = duncteApis.getGuildSetting(guildId)

                map.put("settings", settings)
                map.put("guildColor", Utils.colorToHex(settings.embedColor))

                map.put("guild_patron", fetchGuildPatronStatus(request.guildId!!))
            }

            map.put("hide_settings", false)

            val session = request.session()
            val message: String? = session.attribute(FLASH_MESSAGE)

            if (!message.isNullOrEmpty()) {
                session.attribute(FLASH_MESSAGE, null)
                map.put("message", message)
            } else {
                map.put("message", false)
            }

            map.toModelAndView(view)
        }

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
