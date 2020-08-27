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

package com.dunctebot.discord.api.oauth

import com.dunctebot.discord.api.entities.Guild
import com.dunctebot.discord.api.entities.User
import com.dunctebot.discord.api.exceptions.InvalidStateException
import com.dunctebot.discord.api.oauth.session.Session
import com.dunctebot.discord.api.oauth.session.SessionController
import com.dunctebot.discord.api.oauth.state.StateController
import com.dunctebot.discord.api.rest.actions.Oauth2Action

interface Oauth2Client {
    val id: Long
    val secret: String
    val stateController: StateController
    val sessionController: SessionController<Session>

    fun generateAuthorizationURL(redirectUri: String, vararg scopes: Scope): String

    @Throws(InvalidStateException::class)
    fun startSession(code: String, state: String, identifier: String, vararg scopes: Scope): Oauth2Action<Session>

    // TODO: find out if we want to use the same user object
    fun getUser(session: Session): Oauth2Action<User>

    fun getGuilds(session: Session): Oauth2Action<List<Guild>>

    companion object {
        const val REST_VERSION = 6
    }
}
