package com.dunctebot.dashboard.controllers.api

import com.dunctebot.dashboard.WebServer
import com.dunctebot.dashboard.duncteApis
import com.dunctebot.dashboard.fetchGuild
import com.dunctebot.dashboard.guildId
import com.dunctebot.dashboard.utils.fetchGuildPatronStatus
import com.dunctebot.jda.json.JsonChannel
import com.dunctebot.jda.json.JsonGuild
import com.dunctebot.jda.json.JsonRole
import com.dunctebot.models.settings.GuildSetting
import io.javalin.http.Context
import net.dv8tion.jda.api.entities.TextChannel
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors

object SettingsApiController {
    private val executor = Executors.newSingleThreadExecutor()

    fun get(ctx: Context) {
        val future = CompletableFuture.supplyAsync({
            val data = fetchData(ctx)

            val message: String? = ctx.sessionAttribute(WebServer.FLASH_MESSAGE)

            if (!message.isNullOrEmpty()) {
                ctx.sessionAttribute(WebServer.FLASH_MESSAGE, null)
                data["message"] = message
            } else {
                data["message"] = null
            }

            return@supplyAsync data
        }, executor)

        ctx.future(future)
    }

    fun post(ctx: Context) {
        val body = ctx.bodyValidator<GuildSetting>()
            .check(
                "leave_timeout",
                { it.leaveTimeout in 1..60 },
                "Leave timeout must be within range of 1 to 60 (inclusive)"
            )
            .get()

        ctx.json(body)
    }

    private fun fetchData(ctx: Context): MutableMap<String, Any?> {
        val map = mutableMapOf<String, Any?>()

        val guild = ctx.fetchGuild()

        if (guild != null) {
            val tcs = guild.textChannelCache
                .filter(TextChannel::canTalk)
                .map { JsonChannel(it) }
            val roles = guild.roleCache.filter {
                it.name != "@everyone" && it.name != "@here" && !it.isManaged &&
                    guild.selfMember.canInteract(it)
            }.map { JsonRole(it) }

            map["channels"] = tcs
            map["roles"] = roles
            map["guild"] = JsonGuild(guild)

            val settings = duncteApis.getGuildSetting(guild.idLong) // long

            map["settings"] = settings
            map["patron"] = fetchGuildPatronStatus(ctx.guildId) // string
        }

        return map
    }
}
