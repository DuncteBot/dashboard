package com.dunctebot.dashboard.controllers

import com.dunctebot.dashboard.*
import com.dunctebot.dashboard.rendering.WebVariables
import com.github.benmanes.caffeine.cache.Caffeine
import io.javalin.http.Context
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.exceptions.ErrorResponseException
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

    fun handleOneGuildRegister(ctx: Context) {
        val token = ctx.formParam("token")

        if (token.isNullOrBlank()) {
            renderPatronRegisterPage(ctx) {
                it.put("message", "Submitted token is not valid.")
            }
            return
        }

        if (!securityKeys.containsKey(token)) {
            renderPatronRegisterPage(ctx) {
                it.put("message", "Submitted token is not valid.")
            }
            return
        }

        val userId = ctx.formParam("user_id").toSafeLong()
        val guildId = ctx.formParam("guild_id").toSafeLong()
        val theFormat = "$userId-$guildId"

        if (securityKeys[token] != theFormat) {
            renderPatronRegisterPage(ctx) {
                it.put("message", "Submitted token is not valid.")
            }
            return
        }

        // remove the token as it is used
        securityKeys.remove(token)

        if (duncteApis.isOneGuildPatron(userId.toString())) {
            renderPatronRegisterPage(ctx) {
                it.put("message", "This user is already registered, please contact a bot admin to have it changed.")
            }
            return
        }

        val sendData = jsonMapper.createObjectNode()
            .put("t", "DATA_UPDATE")

        sendData.putObject("d")
            .putObject("new_one_guild")
            .put("user_id", userId)
            .put("guild_id", guildId)

        webSocket.broadcast(sendData)

        renderPatronRegisterPage(ctx) {
            it.put("message", "Server successfully registered.")
                .put("hideForm", true)
        }
    }

    private fun renderPatronRegisterPage(ctx: Context, vars: (WebVariables) -> Unit = {}) {
        val map = WebVariables()

        vars(map)

        map.put("title", "Register your server for patron perks")
            .put("hide_menu", true)
            .put("captcha_sitekey", System.getenv("CAPTCHA_SITEKEY"))

        ctx.render(
            "oneGuildRegister.vm",
            map.toMap()
        )
    }

    fun showGuildRoles(ctx: Context) {
        val hash = ctx.pathParam("hash")
        val guildId = guildHashes.getIfPresent(hash) ?: throw haltNotFound()
        val guild = try {
            // TODO: do we want to do this?
            // Maybe only cache for a short time as it will get outdated data
            restJDA.fakeJDA.getGuildById(guildId) ?: restJDA.retrieveGuildById(guildId.toString()).complete()
        } catch (e: ErrorResponseException) {
            e.printStackTrace()
            throw haltNotFound()
        }

        val roles = guildRoleCache.get(guild.idLong) {
            val members = restJDA.retrieveAllMembers(guild).stream().toList()

            guild.roles.map { CustomRole(it, members) }
        }!!

        ctx.render(
            "guildRoles.vm",
            WebVariables()
                .put("hide_menu", true)
                .put("title", "Roles for ${guild.name}")
                .put("guild_name", guild.name)
                .put("roles", roles)
                .toMap()
        )
    }

    class CustomRole(private val realRole: Role, allMembers: List<Member>) : Role by realRole {
        // Accessed by our templating engine
        @Suppress("unused")
        val memberCount = allMembers.filter { it.roles.contains(realRole) }.size
    }
}
