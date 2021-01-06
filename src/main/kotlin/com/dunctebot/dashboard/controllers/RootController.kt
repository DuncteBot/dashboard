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

import com.dunctebot.dashboard.WebServer.Companion.HOMEPAGE
import com.dunctebot.dashboard.WebServer.Companion.OLD_PAGE
import com.dunctebot.dashboard.WebServer.Companion.SESSION_ID
import com.dunctebot.dashboard.WebServer.Companion.USER_ID
import com.dunctebot.dashboard.duncteApis
import com.jagrosh.jdautilities.oauth2.OAuth2Client
import com.jagrosh.jdautilities.oauth2.Scope
import com.jagrosh.jdautilities.oauth2.exceptions.InvalidStateException
import net.dv8tion.jda.api.exceptions.HttpException
import org.slf4j.LoggerFactory
import spark.Request
import spark.Response

object RootController {
    private val logger = LoggerFactory.getLogger(RootController::class.java)

    fun beforeRoot(request: Request, response: Response, oAuth2Client: OAuth2Client) {
        val ses = request.session()

        if (ses.attribute<String?>(SESSION_ID) == null) {
            val url = oAuth2Client.generateAuthorizationURL(
                System.getenv("OAUTH_REDIRECT_URI"),
                Scope.IDENTIFY, Scope.GUILDS
            )

            ses.attribute(SESSION_ID, "session_${System.currentTimeMillis()}")

            return response.redirect("$url&prompt=none")
        }
    }

    fun callback(request: Request, response: Response, oAuth2Client: OAuth2Client): Any {
        // If we don't have a code from discord
        // and we don't have a state we will return the user to the homepage
        if (!request.queryParams().contains("code") || !request.queryParams().contains("state")) {
            return response.redirect(HOMEPAGE)
        }

        return try {
            // Get the user session
            val session = request.session()

            // If the session is missing we will return the user to the homepage
            if (session.attribute<String?>(SESSION_ID) == null){
                return response.redirect(HOMEPAGE)
            }

            // Get the session id for the user
            val sesid: String? = request.session().attribute(SESSION_ID)
            // Start a session to obtain the oauth2 access token
            val oauthses = oAuth2Client.startSession(
                request.queryParams("code"),
                request.queryParams("state"),
                sesid,
                Scope.IDENTIFY, Scope.GUILDS
            ).complete()

            // Fetch the user from discord
            val userId = oAuth2Client.getUser(oauthses).complete().id

            // TODO: remove when final
            if (request.host() == "olddash.dunctebot.com" && !duncteApis.isPatreon(userId)) {
                return ""
            }

            // Store the user id in the session
            session.attribute(USER_ID, userId)

            // If we have a previous page we will return the user there
            if (session.attributes().contains(OLD_PAGE)) {
                return response.redirect(session.attribute(OLD_PAGE))
            }

            // Otherwise the user will be send to the dashboard homepage
            response.redirect("/")
        } catch (stateEx: InvalidStateException) {
            "<h1>${stateEx.message}</h1><br /><a href=\"${HOMEPAGE}\">Click here to go back home</a>"
        } catch (e: HttpException) {
            logger.error("Failed to log user in with discord", e)

            // If we fail to log in we will return the user back home
            return response.redirect(HOMEPAGE)
        }
    }
}
