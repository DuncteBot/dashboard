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

import com.dunctebot.dashboard.*
import com.dunctebot.dashboard.rendering.DbModelAndView
import com.dunctebot.dashboard.rendering.WebVariables
import com.github.benmanes.caffeine.cache.Caffeine
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.exceptions.ErrorResponseException
import spark.Request
import spark.Response
import java.util.concurrent.TimeUnit
import kotlin.streams.toList

object GuildController {
    // some hash -> "$userId-$guildId"
    val securityKeys = mutableMapOf<String, String>()
    val guildHashes = Caffeine.newBuilder()
        .expireAfterWrite(2, TimeUnit.HOURS)
        .build<String, Long>()

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
            .put("captcha_sitekey", env["CAPTCHA_SITEKEY"]!!)

        return map.toModelAndView("oneGuildRegister.vm")
    }

    fun showGuildRoles(request: Request, response: Response): Any {
        val hash = request.params("hash")
        val guildId = guildHashes.getIfPresent(hash) ?: return haltNotFound(request, response)
        val guild = try {
            // TODO: do we want to do this?
            // Maybe only cache for a short time as it will get outdated data
            restJDA.fakeJDA.getGuildById(guildId) ?: restJDA.retrieveGuildById(guildId.toString()).complete()
        } catch (e: ErrorResponseException) {
            e.printStackTrace()
            return haltNotFound(request, response)
        }

        val members = restJDA.retrieveAllMembers(guild).stream().toList()

//        println("Approximate count: ${guild.memberCount}")
//        println("Actual count: ${members.size}")

        return WebVariables()
            .put("hide_menu", true)
            .put("title", "Roles for ${guild.name}")
            .put("guild_name", guild.name)
            .put("roles", guild.roles.map { CustomRole(it, members) })
            .toModelAndView("guildRoles.vm")
    }

    class CustomRole(private val realRole: Role, allMembers: List<Member>) : Role by realRole {
        // Accessed by our templating engine
        @Suppress("unused")
        val memberCount = allMembers.filter { it.roles.contains(realRole) }.size
    }
}
