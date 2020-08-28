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

package com.dunctebot.jda.impl

import com.dunctebot.jda.MemberPaginationAction
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.exceptions.ParsingException
import net.dv8tion.jda.api.requests.Request
import net.dv8tion.jda.api.requests.Response
import net.dv8tion.jda.internal.entities.GuildImpl
import net.dv8tion.jda.internal.requests.Route
import net.dv8tion.jda.internal.requests.restaction.pagination.PaginationActionImpl

class MemberPaginationActionImpl(override val guild: Guild) :
    PaginationActionImpl<Member, MemberPaginationAction>(
        guild.jda,
        Route.get("guilds/${guild.id}/members").compile(),
        1,
        1000,
        1000
    ), MemberPaginationAction {
    override fun getKey(it: Member): Long  = it.idLong

    override fun finalizeRoute(): Route.CompiledRoute {
        var route = super.finalizeRoute()

        var after: String? = null
        val limit = getLimit().toString()
        val last = this.lastKey

        if (last != 0L) {
            after = last.toString()
        }

        route = route.withQueryParams("limit", limit)

        if (after != null) {
            route = route.withQueryParams("after", after)
        }

        return route
    }

    override fun handleSuccess(response: Response, request: Request<List<Member>>) {
        val builder = api.entityBuilder
        val array = response.array
        val members = mutableListOf<Member>()

        for (i in 0 until array.length()) {
            try {
                val member = builder.createMember(guild as GuildImpl, array.getObject(i))

                members.add(member)

                if (useCache) {
                    cached.add(member)
                }

                last = member
                lastKey = member.idLong
            } catch (e: ParsingException) {
                LOG.warn("Encountered exception in MemberPagination", e)
            } catch (e: NullPointerException) {
                LOG.warn("Encountered exception in MemberPagination", e)
            }
        }

        request.onSuccess(members)
    }
}
