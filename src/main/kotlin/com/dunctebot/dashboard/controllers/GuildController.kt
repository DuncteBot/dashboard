package com.dunctebot.dashboard.controllers

import com.dunctebot.dashboard.*
import com.dunctebot.dashboard.rendering.DbModelAndView
import com.dunctebot.dashboard.rendering.WebVariables
import com.github.benmanes.caffeine.cache.Caffeine
import discord4j.discordjson.json.MemberData
import discord4j.discordjson.json.RoleData
import spark.Request
import spark.Response
import java.util.concurrent.TimeUnit

object GuildController {
    // some hash -> "$userId-$guildId"
    val securityKeys = mutableMapOf<String, String>()
    val guildHashes = Caffeine.newBuilder()
        .expireAfterWrite(2, TimeUnit.HOURS)
        .build<String, Long>()
    val guildRoleCache = Caffeine.newBuilder()
        .expireAfterWrite(1, TimeUnit.HOURS)
        .build<Long, List<CustomRole>>()

    fun handleOneGuildRegister(request: Request): Any {
        val params = request.paramsMap
        val token = params["token"]

        if (token.isNullOrBlank()) {
            return renderPatronRegisterPage {
                it.put("message", "Submitted token is not valid.")
            }
        }

        if (!securityKeys.containsKey(token)) {
            return renderPatronRegisterPage {
                it.put("message", "Submitted token is not valid.")
            }
        }

        val userId = params["user_id"].toSafeLong()
        val guildId = params["guild_id"].toSafeLong()
        val theFormat = "$userId-$guildId"

        if (securityKeys[token] != theFormat) {
            return renderPatronRegisterPage {
                it.put("message", "Submitted token is not valid.")
            }
        }

        // remove the token as it is used
        securityKeys.remove(token)

        if (duncteApis.isOneGuildPatron(userId.toString())) {
            return renderPatronRegisterPage {
                it.put("message", "This user is already registered, please contact a bot admin to have it changed.")
            }
        }

        val sendData = jsonMapper.createObjectNode()
            .put("t", "DATA_UPDATE")

        sendData.putObject("d")
            .putObject("new_one_guild")
            .put("user_id", userId)
            .put("guild_id", guildId)

        webSocket.broadcast(sendData)

        return renderPatronRegisterPage {
            it.put("message", "Server successfully registered.")
                .put("hideForm", true)
        }
    }

    private fun renderPatronRegisterPage(vars: (WebVariables) -> Unit = {}): DbModelAndView {
        val map = WebVariables()

        vars(map)

        map.put("title", "Register your server for patron perks")
            .put("hide_menu", true)
            .put("captcha_sitekey", System.getenv("CAPTCHA_SITEKEY"))

        return map.toModelAndView("oneGuildRegister.vm")
    }

    fun showGuildRoles(request: Request, response: Response): Any {
        val hash = request.params("hash")
        val guildId = guildHashes.getIfPresent(hash) ?: return haltNotFound(request, response)

        val roles = guildRoleCache.get(guildId) {
            val internalRoles = discordClient.retrieveGuildRoles(guildId)
            val members = discordClient.retrieveGuildMembers(guildId).collectList().block()!!

            internalRoles.map { CustomRole(it, members) }.collectList().block()
        }!!

        val guildName = "TEMP GUILD NAME"

        return WebVariables()
            .put("hide_menu", true)
            .put("title", "Roles for $guildName")
            .put("guild_name", guildName)
            .put("roles", roles)
            .toModelAndView("guildRoles.vm")
    }

    class CustomRole(private val realRole: RoleData, allMembers: List<MemberData>) : RoleData by realRole {
        // Accessed by our templating engine
        @Suppress("unused")
        val memberCount = allMembers.filter { it.roles().contains(realRole.id()) }
    }
}
