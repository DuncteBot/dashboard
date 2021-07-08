package com.dunctebot.dashboard.controllers

import com.dunctebot.dashboard.WebServer.Companion.OLD_PAGE
import com.dunctebot.dashboard.WebServer.Companion.SESSION_ID
import com.dunctebot.dashboard.WebServer.Companion.USER_ID
import com.dunctebot.dashboard.fetchGuild
import com.dunctebot.dashboard.guildId
import com.dunctebot.dashboard.discordClient
import com.dunctebot.dashboard.userId
import net.dv8tion.jda.api.Permission
import spark.Request
import spark.Response
import spark.Spark

object DashboardController {
    fun before(request: Request, response: Response) {
        val ses = request.session()

        if (ses.attribute<String?>(USER_ID) == null || ses.attribute<String?>(SESSION_ID) == null) {
            request.session().attribute(OLD_PAGE, request.pathInfo())
            return response.redirect("/")
        }

        val guild = request.fetchGuild()
                ?: throw Spark.halt(404, "DuncteBot is not in the requested server, why don't you <a href=\"https://discord.com/oauth2" +
                    "/authorize?client_id=210363111729790977&guild_id=${request.guildId}" +
                    "&scope=bot&permissions=1609952470\" target=\"_blank\">invite it</a>?")

        val member = try {
            discordClient.retrieveMemberById(guild, request.userId).complete()
        } catch (e: Exception) {
            e.printStackTrace()
            throw Spark.halt(200, "<h1>Either discord did a fucky wucky or you are not in the server that you are trying to edit</h1>")
        }

        if (!member.hasPermission(Permission.MANAGE_SERVER)) {
            Spark.halt(200, "<h1>You do not have permission to edit this server</h1>")
        }
    }
}
