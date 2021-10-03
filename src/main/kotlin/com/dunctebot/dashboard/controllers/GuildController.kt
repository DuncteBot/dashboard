package com.dunctebot.dashboard.controllers

import com.dunctebot.dashboard.*
import com.dunctebot.dashboard.rendering.WebVariables
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.github.benmanes.caffeine.cache.Caffeine
import io.javalin.http.Context
import io.javalin.http.NotFoundResponse
import io.javalin.plugin.rendering.vue.VueComponent
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
        .build<Long, CustomRoleList>()

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
        val guildId = guildHashes.getIfPresent(hash) ?: throw NotFoundResponse()
        val guild = try {
            // TODO: do we want to do this?
            // Maybe only cache for a short time as it will get outdated data
            restJDA.fakeJDA.getGuildById(guildId) ?: restJDA.retrieveGuildById(guildId.toString()).complete()
        } catch (e: ErrorResponseException) {
            e.printStackTrace()
            throw NotFoundResponse()
        }

        // populate the cache if needed
        guildRoleCache.get(guild.idLong) {
            val members = restJDA.retrieveAllMembers(guild).stream().toList()

            CustomRoleList(guild.name, guild.roles.map { CustomRole(it, members) })
        }!!

        // terrible way of doing this, but it works well enough
        // we're sending the decoded guild id into the state of javalin
        VueComponent("roles", mapOf("guildId" to guild.id)).handle(ctx)
    }

    fun guildRolesApiHandler(ctx: Context) {
        val guildId = ctx.pathParam("guildId").toLong()
        // will ensure that the cache is validated and we can't randomly request guilds
        val cache = guildRoleCache.getIfPresent(guildId) ?: throw NotFoundResponse()

        ctx.json(cache)
    }

    @Suppress("unused")
    class CustomRoleList(val guildName: String, val roles: List<CustomRole>)

    // yikes
    @JsonIgnoreProperties(value = ["jda", "color", "guild", "manager", "tags", "permissions", "permissionsExplicit", "permissionsRaw", "timeCreated", "idLong", "asMention", "managed", "hoisted", "mentionable", "publicRole", "positionRaw"])
    class CustomRole(private val realRole: Role, allMembers: List<Member>) : Role by realRole {
        @JsonInclude
        @Suppress("unused")
        val memberCount = allMembers.filter {
            this.name == "@everyone" || it.roles.contains(realRole)
        }.size
    }
}
