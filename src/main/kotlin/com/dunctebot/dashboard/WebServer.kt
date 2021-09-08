package com.dunctebot.dashboard

import com.dunctebot.dashboard.controllers.DashboardController
import com.dunctebot.dashboard.controllers.GuildController
import com.dunctebot.dashboard.controllers.RootController
import com.dunctebot.dashboard.controllers.SettingsController
import com.dunctebot.dashboard.controllers.api.CustomCommandController
import com.dunctebot.dashboard.controllers.api.DataController
import com.dunctebot.dashboard.controllers.api.GuildApiController
import com.dunctebot.dashboard.controllers.api.OtherAPi
import com.dunctebot.dashboard.rendering.VelocityRenderer
import com.dunctebot.dashboard.rendering.WebVariables
import com.dunctebot.dashboard.utils.fetchGuildPatronStatus
import com.dunctebot.jda.oauth.OauthSessionController
import com.dunctebot.models.settings.GuildSetting
import com.dunctebot.models.settings.ProfanityFilterType
import com.dunctebot.models.settings.WarnAction
import com.dunctebot.models.utils.Utils
import com.jagrosh.jdautilities.oauth2.OAuth2Client
import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.core.compression.CompressionStrategy
import io.javalin.http.staticfiles.Location
import io.javalin.plugin.rendering.JavalinRenderer
import net.dv8tion.jda.api.entities.TextChannel

class WebServer {
    private val engine = VelocityRenderer()
    private val app: Javalin
    val oAuth2Client: OAuth2Client = OAuth2Client.Builder()
        .setSessionController(OauthSessionController())
        .setClientId(System.getenv("OAUTH_CLIENT_ID").toLong())
        .setClientSecret(System.getenv("OAUTH_CLIENT_SECRET"))
        .build()

    init {
        // Register the view renderer
        JavalinRenderer.register(engine::render, ".vm")

        this.app = Javalin.create { config ->
            config.compressionStrategy(CompressionStrategy.GZIP)
            config.autogenerateEtags = true
            config.showJavalinBanner = false

            if (System.getenv("IS_LOCAL").toBoolean()) {
                val projectDir = System.getProperty("user.dir")
                val staticDir = "/src/main/resources/public"
                config.addStaticFiles(projectDir + staticDir, Location.EXTERNAL)
                config.enableDevLogging()
            } else {
                config.addStaticFiles("/public", Location.CLASSPATH)
            }
        }

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

        this.app.routes {
            path("server/$GUILD_ID") {
                before { ctx -> DashboardController.before(ctx) }
                before("") { ctx -> DashboardController.before(ctx) }

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

                post { ctx -> SettingsController.saveSettings(ctx) }

                // Custom command settings
                getWithGuildData(
                    "custom-commands",
                    WebVariables().put("title", "Dashboard")
                        .put("using_tabs", false),
                    "dashboard/customCommandSettings.vm"
                )
            }
        }
    }

    private fun addAPIRoutes() {
        this.app.routes {
            path("/api") {
                get("/user-guilds") { ctx -> OtherAPi.fetchGuildsOfUser(ctx, oAuth2Client) }

                // This is just used by uptime robot to check if the application is up
                get("/uptimerobot") { ctx -> OtherAPi.uptimeRobot(ctx) }

                // keep?
                get("/commands.json") { ctx ->
                    ctx.plainText()
                        .result("TODO: setup websocket to bot")
                }

                post("/update-data") { ctx -> DataController.updateData(ctx) }
                get("/invalidate-tokens") { ctx -> DataController.invalidateTokens(ctx)  }

                path("/check") {
                    post("/user-guild") { ctx ->
                        GuildApiController.findUserAndGuild(ctx)
                    }
                }

                path("/custom-commands/$GUILD_ID") {
                    before("") { ctx -> CustomCommandController.before(ctx) }
                    get { ctx -> CustomCommandController.show(ctx) }
                    patch { ctx -> CustomCommandController.update(ctx) }
                    post { ctx -> CustomCommandController.create(ctx) }
                    delete { ctx -> CustomCommandController.delete(ctx) }
                }
            }
        }
    }

    private fun mapErrorRoutes() {
        this.app.error(404) { ctx ->
            ctx.render(
                "errors/404.vm",
                WebVariables()
                    .put("hide_menu", true)
                    .put("title", "404 - Page Not Found")
                    .toMap()
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
        get(path) { ctx ->
            val guild = ctx.fetchGuild()

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

                map.put("guild_patron", fetchGuildPatronStatus(ctx.guildId))
            }

            val message: String? = ctx.sessionAttribute(FLASH_MESSAGE)

            if (!message.isNullOrEmpty()) {
                ctx.sessionAttribute(FLASH_MESSAGE, null)
                map.put("message", message)
            } else {
                map.put("message", false)
            }

            map.put("hide_menu", false)

            ctx.render(view, map.toMap())
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
