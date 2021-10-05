package com.dunctebot.dashboard.controllers.api

import com.dunctebot.dashboard.WebServer
import com.dunctebot.dashboard.duncteApis
import com.dunctebot.dashboard.fetchGuild
import com.dunctebot.dashboard.guildId
import com.dunctebot.dashboard.utils.fetchGuildPatronStatus
import com.dunctebot.jda.json.JsonChannel
import com.dunctebot.jda.json.JsonGuild
import com.dunctebot.jda.json.JsonRole
import io.javalin.http.Context
import net.dv8tion.jda.api.entities.TextChannel

object SettingsApiController {
    fun get(ctx: Context) {
        val data = fetchData(ctx)

        val message: String? = ctx.sessionAttribute(WebServer.FLASH_MESSAGE)

        if (!message.isNullOrEmpty()) {
            ctx.sessionAttribute(WebServer.FLASH_MESSAGE, null)
            data["message"] = message
        } else {
            data["message"] = null
        }

        ctx.json(data)
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
