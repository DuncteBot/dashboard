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
import com.dunctebot.dashboard.rendering.WebVariables
import com.dunctebot.dashboard.utils.fetchGuildPatronStatus
import com.dunctebot.dashboard.utils.getEffectivePermissions
import com.dunctebot.models.settings.GuildSetting
import com.dunctebot.models.settings.ProfanityFilterType
import com.dunctebot.models.settings.WarnAction
import com.dunctebot.models.utils.Utils
import com.jagrosh.jdautilities.oauth2.OAuth2Client
import discord4j.common.util.Snowflake
import discord4j.rest.util.Permission
import discord4j.rest.util.PermissionSet
import spark.Spark.*

// The socket server will be used to communicate with DuncteBot himself
// NGINX can secure the websocket (hopefully it does this by default as we are using the same domain)
class WebServer {
    private val oAuth2Client = OAuth2Client.Builder()
        .setClientId(System.getenv("OAUTH_CLIENT_ID").toLong())
        .setClientSecret(System.getenv("OAUTH_CLIENT_SECRET"))
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
        if (System.getenv("IS_LOCAL").toBoolean()) {
            val projectDir = System.getProperty("user.dir")
            val staticDir = "/src/main/resources/public"
            staticFiles.externalLocation(projectDir + staticDir)
            port(2000)
        } else {
            staticFiles.location("/public")
        }

        defaultResponseTransformer { transformResponse(it) }

        /*get("/test") { request, response ->
            println(request.host())
            println(request.url()) // split on the last / and append callback
            println(request.uri())

            "check console"
        }*/

        // Non settings related routes
        get("/roles/:hash") { request, response ->
            return@get GuildController.showGuildRoles(request, response)
        }

        get("/register-server") { _, _ ->
            WebVariables()
                .put("hide_menu", true)
                .put("title", "Register your server for patron perks")
                .put("captcha_sitekey", System.getenv("CAPTCHA_SITEKEY"))
                .toModelAndView("oneGuildRegister.vm")
        }

        post("/register-server") { request, _ ->
            return@post GuildController.handleOneGuildRegister(request)
        }

        addDashboardRoutes()
        addAPIRoutes()

        notFound { request, response ->
            val result = HttpErrorHandlers.notFound(request, response)

            return@notFound transformResponse(result)
        }

        internalServerError { request, response ->
            val result = HttpErrorHandlers.internalServerError(request, response)

            return@internalServerError transformResponse(result)
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
                return@before RootController.beforeRoot(request, response, oAuth2Client)
            }

            get("") { _, _ ->
                return@get WebVariables()
                    .put("title", "Dashboard")
                    .put("hide_menu", true)
                    .toModelAndView("dashboard/index.vm")
            }
        }

        path("/server/$GUILD_ID") {
            before("/*") { request, response ->
                return@before DashboardController.before(request, response)
            }

            before("") { request, response ->
                return@before DashboardController.before(request, response)
            }

            getWithGuildData(
                "",
                WebVariables().put("title", "Dashboard")
                    .put("filterValues", ProfanityFilterType.values())
                    .put("warnActionTypes", WarnAction.Type.values())
                    .put("loggingTypes", GuildSetting.LOGGING_TYPES)
                    .put("patronMaxWarnActions", WarnAction.PATRON_MAX_ACTIONS)
                    .put("using_tabs", true),
                "dashboard/serverSettings.vm"
            )

            post("") { request, response ->
                return@post SettingsController.saveSettings(request, response)
            }

            // Custom command settings
            getWithGuildData(
                "/custom-commands",
                WebVariables().put("title", "Dashboard")
                    .put("using_tabs", false),
                "dashboard/customCommandSettings.vm"
            )


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

            // This is just used by uptime robot to check if the application is up
            get("/uptimerobot") { _, _ ->
                return@get OtherAPi.uptimeRobot()
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

    fun start() {
        // dummy method for class loading
    }

    fun shutdown() {
        awaitStop()
    }

    private fun getWithGuildData(path: String, map: WebVariables, view: String) {
        get(path) { request, _ ->
            val guild = request.guild

            if (guild != null) {
                val guildId = guild.id.asLong()
                val self = guild.selfMember.block()!!
                val selfId = Snowflake.of(self.user().id())

                val tcs = guild.channels
                    .filter {
                        it.getEffectivePermissions(guild, self).map { p ->
                            println("Permissions $p")
                            p.containsAll(PermissionSet.of(
                                Permission.SEND_MESSAGES, Permission.VIEW_CHANNEL /* read messages */
                            ))
                        }.block()!!
                    }
                    .collectList()
                    .block()!!

                println("channels $tcs")

                val goodRoles = guild.roles
                    .filter { !it.managed() }
                    .filter { it.name() != "@everyone" && it.name() != "@here" }
                    // TODO: check if can interact
                    .collectList()
                    .block()!!

                /*val goodRoles_old = guild.roleCache.filter {
                    guild.selfMember.canInteract(it) && it.name != "@everyone" && it.name != "@here"
                }.filter { !it.isManaged }.toList()*/

                map.put("goodChannels", tcs)
                map.put("goodRoles", goodRoles)
                map.put("guild", discordClient.retrieveGuildData(guildId))

                val settings = duncteApis.getGuildSetting(guildId)

                map.put("settings", settings)
                map.put("guildColor", Utils.colorToHex(settings.embedColor))

                map.put("guild_patron", fetchGuildPatronStatus(request.guildId!!))
            }

            val session = request.session()
            val message: String? = session.attribute(FLASH_MESSAGE)

            if (!message.isNullOrEmpty()) {
                session.attribute(FLASH_MESSAGE, null)
                map.put("message", message)
            } else {
                map.put("message", false)
            }

            map.put("hide_menu", false)

            map.toModelAndView(view)
        }

    }

    companion object {
        const val FLASH_MESSAGE = "FLASH_MESSAGE"
        const val OLD_PAGE = "OLD_PAGE"
        const val SESSION_ID = "sessionId"
        const val USER_ID = "USER_SESSION"
        const val GUILD_ID = ":guildid"
        const val HOMEPAGE = "https://www.duncte.bot/"
    }
}
