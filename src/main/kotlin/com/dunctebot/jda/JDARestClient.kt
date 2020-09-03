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

package com.dunctebot.jda

import com.dunctebot.jda.impl.MemberPaginationActionImpl
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.SelfUser
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.requests.RestAction
import net.dv8tion.jda.api.utils.MiscUtil
import net.dv8tion.jda.api.utils.data.DataArray
import net.dv8tion.jda.internal.JDAImpl
import net.dv8tion.jda.internal.requests.CompletedRestAction
import net.dv8tion.jda.internal.requests.RestActionImpl
import net.dv8tion.jda.internal.requests.Route
import net.dv8tion.jda.internal.utils.config.AuthorizationConfig
import net.dv8tion.jda.internal.utils.config.MetaConfig
import net.dv8tion.jda.internal.utils.config.SessionConfig
import net.dv8tion.jda.internal.utils.config.ThreadingConfig
import java.util.concurrent.Executors


/**
 * Custom jda rest client that allows for rest-only usage of JDA
 *
 * This class has been inspired by GivawayBot and all credit goes to them https://github.com/jagrosh/GiveawayBot
 */
class JDARestClient(token: String) {
    private val jda: JDAImpl

    init {
        val authConfig = AuthorizationConfig(token)
        val sessionConfig = SessionConfig.getDefault()
        val threadConfig = ThreadingConfig.getDefault()
        val metaConfig = MetaConfig.getDefault()

        threadConfig.setRateLimitPool(Executors.newScheduledThreadPool(5) {
            val t = Thread(it, "dunctebot-rest-thread")
            t.isDaemon = true
            return@newScheduledThreadPool t
        }, true)

        jda = JDAImpl(authConfig, sessionConfig, threadConfig, metaConfig)

        retrieveSelfUser().queue(jda::setSelfUser)
    }

    // is public for JDA utils
    val fakeJDA = FakeJDA(this, jda)

    fun retrieveUserById(id: String): RestAction<User> {
        val route = Route.Users.GET_USER.compile(id)

        return RestActionImpl(jda, route) {
            response, _ -> jda.entityBuilder.createUser(response.getObject())
        }
    }

    private fun retrieveSelfUser(): RestAction<SelfUser> {
        val route = Route.Self.GET_SELF.compile()

        return RestActionImpl(jda, route) {
            response, _ -> jda.entityBuilder.createSelfUser(response.getObject())
        }
    }

    fun retrieveAllMembers(guild: Guild): MemberPaginationAction = MemberPaginationActionImpl(guild)

    private fun retrieveGuildChannelsArray(guildId: String): RestAction<DataArray> {
        val route = Route.Guilds.GET_CHANNELS.compile(guildId)

        return RestActionImpl(jda, route) { response, _ -> response.array}
    }

    fun retrieveGuildById(id: String): RestAction<Guild> {
        // Lookup the guild from the cache
        val guildById = jda.getGuildById(id)

        // If we already have it we will return the cached guild
        // TODO: invalidation of caches
        if (guildById != null) {
            return CompletedRestAction(jda, guildById)
        }

        val route = Route.Guilds.GET_GUILD.compile(id).withQueryParams("with_counts", "true")

        return retrieveGuildChannelsArray(id).flatMap { channels ->
            RestActionImpl(jda, route) { response, _ ->
                val data = response.getObject()

                // fake a bit of data
                data.put("channels", channels)
                data.put("voice_states", DataArray.empty())

                jda.entityBuilder.createGuild(id.toLong(), data, MiscUtil.newLongMap(), data.getInt("approximate_member_count"))
            }
        }
    }
}
