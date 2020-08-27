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

import net.dv8tion.jda.api.GatewayEncoding
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.SelfUser
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.requests.RestAction
import net.dv8tion.jda.api.utils.ChunkingFilter
import net.dv8tion.jda.api.utils.Compression
import net.dv8tion.jda.api.utils.MiscUtil
import net.dv8tion.jda.api.utils.data.DataArray
import net.dv8tion.jda.api.utils.data.DataObject
import net.dv8tion.jda.internal.JDAImpl
import net.dv8tion.jda.internal.requests.RestActionImpl
import net.dv8tion.jda.internal.requests.Route
import net.dv8tion.jda.internal.requests.WebSocketClient
import net.dv8tion.jda.internal.utils.config.AuthorizationConfig
import net.dv8tion.jda.internal.utils.config.MetaConfig
import net.dv8tion.jda.internal.utils.config.SessionConfig
import net.dv8tion.jda.internal.utils.config.ThreadingConfig
import net.dv8tion.jda.internal.utils.config.flags.ConfigFlag
import okhttp3.OkHttpClient
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
        val sessionConfig = SessionConfig(
            FakeSessionController(),
            OkHttpClient(),
            null, null,
            ConfigFlag.getDefault(),
            900, 250
        )
        val threadConfig = ThreadingConfig.getDefault()
        val metaConfig = MetaConfig.getDefault()

        threadConfig.setRateLimitPool(Executors.newScheduledThreadPool(5) {
            val t = Thread(it, "dunctebot-rest-thread")
            t.isDaemon = true
            return@newScheduledThreadPool t
        }, true)

        jda = JDAImpl(authConfig, sessionConfig, threadConfig, metaConfig)

        jda.setChunkingFilter(ChunkingFilter.NONE)
        setWebsocketClientOnJDA()

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

    private fun setWebsocketClientOnJDA() {
        val cls = jda.javaClass

        val field = cls.getDeclaredField("client")

        field.isAccessible = true

        field.set(jda, WebSocketClient(jda, Compression.ZLIB, GatewayIntent.ALL_INTENTS, GatewayEncoding.ETF))
    }

    private fun retrieveSelfUser(): RestAction<SelfUser> {
        val route = Route.Self.GET_SELF.compile()

        return RestActionImpl(jda, route) {
            response, _ -> jda.entityBuilder.createSelfUser(response.getObject())
        }
    }

    // after is a snowflake id dummy
    private fun retrieveGuildMembersArray(guildId: String, limit: Int = 1000, after: String = "0"): RestAction<DataArray> {
        val route = Route.get("guilds/$guildId/members").compile().withQueryParams(
            "limit", limit.toString(),
            "after", after
        )

        return RestActionImpl(jda, route) { response, _ -> response.array}
    }

    private fun retrieveGuildChannelsArray(guildId: String): RestAction<DataArray> {
        val route = Route.Guilds.GET_CHANNELS.compile(guildId)

        return RestActionImpl(jda, route) { response, _ -> response.array}
    }

    fun retrieveGuildById(id: String, withMembers: Boolean = false): RestAction<Guild> {
        val route = Route.Guilds.GET_GUILD.compile(id).withQueryParams("with_counts", "true")
        val membersMap = MiscUtil.newLongMap<DataObject>()

        if (withMembers) {
            // oh god, we start chunking the members here
            var lastId = "0"

            do {
                val members = retrieveGuildMembersArray(id, after = lastId).complete()

                if (members.isEmpty) {
                    break
                }

                val currentLastId = members.getObject(members.length() - 1).getObject("user").getString("id")

                if (currentLastId == lastId) {
                    break
                }

                lastId = currentLastId

                for (i in 0 until members.length()) {
                    val obj = members.getObject(i)

                    membersMap.put(obj.getObject("user").getLong("id"), obj)
                }

            } while (true)
        }

        return RestActionImpl(jda, route) { response, _ ->
            val data = response.getObject()

            // fake a bit of data
            data.put("channels", retrieveGuildChannelsArray(id).complete())
            data.put("voice_states", DataArray.empty())

            jda.entityBuilder.createGuild(
                id.toLong(),
                data,
                membersMap,
                data.getInt("approximate_member_count")
            )
        }
    }
}
