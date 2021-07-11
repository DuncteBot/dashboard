package com.dunctebot.dashboard.controllers

import com.dunctebot.dashboard.*
import com.dunctebot.dashboard.WebServer.Companion.OLD_PAGE
import com.dunctebot.dashboard.WebServer.Companion.SESSION_ID
import com.dunctebot.dashboard.WebServer.Companion.USER_ID
import com.dunctebot.discord.extensions.hasPermission
import discord4j.rest.util.Permission
import spark.Request
import spark.Response
import spark.Spark

object DashboardController {
    fun before(request: Request, response: Response) {
        val ses = request.session()

        if (ses.attribute<String?>(USER_ID) == null || ses.attribute<String?>(SESSION_ID) == null) {
            request.session().attribute(OLD_PAGE, request.pathInfo())
            response.redirect("/")

            throw Spark.halt()
        }

        val guild = request.fetchGuild() ?: throw haltDiscordError(DiscordError.NO_GUILD, request.guildId!!)
        val guildId = guild.id().asLong()

        val member = try {
            discordClient.retrieveMemberById(guildId, request.userId).block()!!
        } catch (e: Exception) {
            e.printStackTrace()
            throw haltDiscordError(DiscordError.WAT)
        }

        if (!member.hasPermission(guildId, Permission.MANAGE_GUILD)) {
            throw haltDiscordError(DiscordError.NO_PERMS)
        }
    }
}
