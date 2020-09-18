/*
 * MIT License
 *
 * Copyright (c) 2020 Duncan Sterken
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.dunctebot.dashboard.controllers

import com.dunctebot.dashboard.WebServer.Companion.GUILD_ID
import com.dunctebot.dashboard.WebServer.Companion.OLD_PAGE
import com.dunctebot.dashboard.WebServer.Companion.SESSION_ID
import com.dunctebot.dashboard.WebServer.Companion.USER_ID
import com.dunctebot.dashboard.fetchGuild
import com.dunctebot.dashboard.rendering.WebVariables
import com.dunctebot.dashboard.restJDA
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

        if (guild == null) {
            val guildId = request.params(GUILD_ID)

            throw Spark.halt(404, "DuncteBot is not in the requested server, why don't you <a href=\"https://discord.com/oauth2" +
                "/authorize?client_id=210363111729790977&guild_id=$guildId" +
                "&scope=bot&permissions=1609952470\" target=\"_blank\">invite it</a>?")
        }

        val member = try {
            restJDA.retrieveMemberById(guild, request.userId).complete()
        } catch (e: Exception) {
            e.printStackTrace()
            throw Spark.halt(200, "<h1>Either discord did a fucky wucky or you are not in the server that you are trying to edit</h1>")
        }

        if (!member.hasPermission(Permission.MANAGE_SERVER)) {
            Spark.halt(200, "<h1>You do not have permission to edit this server</h1>")
        }
    }

    fun guildSettingSelect(request: Request): Any {
        val guild = request.fetchGuild();

        return WebVariables()
            .put("title", "Dashboard")
            .put("id", request.params(GUILD_ID))
            .put("name", guild?.name ?: "wot?")
            .put("guild", guild ?: "")
            .toModelAndView("dashboard/panelSelection.vm")
    }
}
