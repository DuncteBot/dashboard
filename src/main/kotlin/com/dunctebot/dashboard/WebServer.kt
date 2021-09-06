package com.dunctebot.dashboard

import com.dunctebot.dashboard.controllers.DashboardController
import com.dunctebot.dashboard.controllers.GuildController
import com.dunctebot.dashboard.controllers.RootController
import com.dunctebot.dashboard.controllers.SettingsController
import com.dunctebot.dashboard.controllers.api.CustomCommandController
import com.dunctebot.dashboard.controllers.api.DataController
import com.dunctebot.dashboard.controllers.api.GuildApiController
import com.dunctebot.dashboard.controllers.api.OtherAPi
import com.dunctebot.dashboard.rendering.WebVariables
import com.dunctebot.dashboard.utils.fetchGuildPatronStatus
import com.dunctebot.models.settings.GuildSetting
import com.dunctebot.models.settings.ProfanityFilterType
import com.dunctebot.models.settings.WarnAction
import com.dunctebot.models.utils.Utils
import com.jagrosh.jdautilities.oauth2.OAuth2Client
import io.javalin.Javalin
import io.javalin.core.compression.CompressionStrategy
import io.javalin.http.staticfiles.Location
import io.javalin.plugin.rendering.JavalinRenderer
import net.dv8tion.jda.api.entities.TextChannel
import spark.Spark.*

// The socket server will be used to communicate with DuncteBot himself
// NGINX can secure the websocket (hopefully it does this by default as we are using the same domain)
class WebServer {
    private val app: Javalin
    private val oAuth2Client = OAuth2Client.Builder()
        .setClientId(System.getenv("OAUTH_CLIENT_ID").toLong())
        .setClientSecret(System.getenv("OAUTH_CLIENT_SECRET"))
        .build()

    init {
        // Register the view renderer
        JavalinRenderer.register(engine::render, ".vm")

        this.app = Javalin.create { config ->
            config.compressionStrategy(CompressionStrategy.GZIP)
            config.autogenerateEtags = true

            if (System.getenv("IS_LOCAL").toBoolean()) {
                val projectDir = System.getProperty("user.dir")
                val staticDir = "/src/main/resources/public"
                config.addStaticFiles(projectDir + staticDir, Location.EXTERNAL)
                config.enableDevLogging()
            } else {
                config.addStaticFiles("/public", Location.CLASSPATH)
            }
        }

        port(1234)

        defaultResponseTransformer { transformResponse(it) }

        /*get("/test") { request, response ->
            println(request.host())
            println(request.url()) // split on the last / and append callback
            println(request.uri())

            "check console"
        }*/

        // Non settings related routes
        this.app.get("/roles/:hash") { ctx -> GuildController.showGuildRoles(ctx) }

        this.app.get("/register-server") { ctx ->
            ctx.render(
                "oneGuildRegister.vm",
                WebVariables()
                    .put("hide_menu", true)
                    .put("title", "Register your server for patron perks")
                    .put("captcha_sitekey", System.getenv("CAPTCHA_SITEKEY"))
                    .toMap()
            )
        }

        this.app.post("/register-server") { ctx -> GuildController.handleOneGuildRegister(ctx) }

        addDashboardRoutes()
        addAPIRoutes()
        mapErrorRoutes()
    }

    private fun addDashboardRoutes() {
        this.app.get("/callback") { ctx -> RootController.callback(ctx, oAuth2Client) }

        this.app.get("/logout") { ctx ->
            // Dear Intellij, shut the fuck up this code compiles
            ctx.req.session.invalidate()

            ctx.redirect("$HOMEPAGE?logout=true")
        }

        this.app.before("/") { ctx -> RootController.beforeRoot(ctx, oAuth2Client) }
        this.app.get("/") { ctx ->
            ctx.render(
                "dashboard/index.vm",
                WebVariables()
                    .put("title", "Dashboard")
                    .put("hide_menu", true)
                    .toMap()
            )
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
                return@get OtherAPi.fetchGuildsOfUser(request, response, oAuth2Client)
            }

            // This is just used by uptime robot to check if the application is up
            get("/uptimerobot") { _, _ ->
                return@get OtherAPi.uptimeRobot()
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

    private fun mapErrorRoutes() {
        this.app.error(404, "json") { ctx ->
            ctx.json(
                jsonMapper.createObjectNode()
                    .put("success", false)
                    .put("message", "'${ctx.path()}' was not found")
                    .put("code", ctx.status())
            )
        }

        this.app.error(404) { ctx ->
            ctx.render(
                "errors/404.vm",
                WebVariables()
                    .put("hide_menu", true)
                    .put("title", "404 - Page Not Found")
                    .toMap()
            )
        }

        this.app.error(500, "json") { ctx ->
            ctx.json(
                jsonMapper.createObjectNode()
                    .put("success", false)
                    .put("message", "Internal server error")
                    .put("code", ctx.status())
            )
        }

        this.app.error(500) { ctx ->
            ctx.render(
                "errors/500.vm",
                WebVariables()
                    .put("hide_menu", true)
                    .put("title", "500 - Internal Server error")
                    .toMap()
            )
        }
    }

    fun start() {
        if (System.getenv("IS_LOCAL").toBoolean()) {
            this.app.start(2000)
        } else {
            this.app.start(4567)
        }
    }

    fun shutdown() {
        this.app.stop()
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
