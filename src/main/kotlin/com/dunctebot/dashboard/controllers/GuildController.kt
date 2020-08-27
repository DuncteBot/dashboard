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

import com.dunctebot.dashboard.WebHelpers
import com.dunctebot.dashboard.jda
import com.dunctebot.dashboard.rendering.WebVariables
import com.github.benmanes.caffeine.cache.Caffeine
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.exceptions.ErrorResponseException
import net.dv8tion.jda.api.utils.cache.MemberCacheView
import spark.Request
import spark.Response
import java.util.concurrent.TimeUnit

object GuildController {
    val guildHashes = Caffeine.newBuilder()
        .expireAfterWrite(2, TimeUnit.HOURS)
        .build<String, Long>()

    init {
        guildHashes.put("test", 191245668617158656L)
    }

    fun showGuildRoles(request: Request, response: Response): Any {
        val hash = request.params("hash")
        val guildId = guildHashes.getIfPresent(hash) ?: return WebHelpers.haltNotFound(request, response)
        val guild = try {
            // TODO: do we want to do this?
            // Maybe only cache for a short time as it will get outdated
            jda.fakeJDA.getGuildById(guildId) ?: jda.retrieveGuildById(guildId.toString(), true).complete()
        } catch (e: ErrorResponseException) {
            e.printStackTrace()
            return WebHelpers.haltNotFound(request, response)
        }

        return WebVariables()
            .put("title", "Roles for ${guild.name}")
            .put("guild_name", guild.name)
            .put("roles", guild.roles.map { CustomRole(it, guild.memberCache) })
            .toModelAndView("guildRoles.vm")
    }

    class CustomRole(private val realRole: Role, allMembers: MemberCacheView) : Role by realRole {
        // Accessed by our templating engine
        @Suppress("unused")
        val memberCount = allMembers.filter { it.roles.contains(realRole) }.size
    }
}
