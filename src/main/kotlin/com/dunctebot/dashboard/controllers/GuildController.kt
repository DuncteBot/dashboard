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

import com.dunctebot.dashboard.haltNotFound
import com.dunctebot.dashboard.rendering.WebVariables
import com.dunctebot.dashboard.restJDA
import com.github.benmanes.caffeine.cache.Caffeine
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.exceptions.ErrorResponseException
import spark.Request
import spark.Response
import java.util.concurrent.TimeUnit
import kotlin.streams.toList

object GuildController {
    val guildHashes = Caffeine.newBuilder()
        .expireAfterWrite(2, TimeUnit.HOURS)
        .build<String, Long>()

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
