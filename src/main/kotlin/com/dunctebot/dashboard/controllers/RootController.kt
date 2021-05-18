package com.dunctebot.dashboard.controllers

import com.dunctebot.dashboard.WebServer.Companion.HOMEPAGE
import com.dunctebot.dashboard.WebServer.Companion.OLD_PAGE
import com.dunctebot.dashboard.WebServer.Companion.SESSION_ID
import com.dunctebot.dashboard.WebServer.Companion.USER_ID
import com.jagrosh.jdautilities.oauth2.OAuth2Client
import com.jagrosh.jdautilities.oauth2.OAuth2Client.DISCORD_REST_VERSION
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
            var url = oAuth2Client.generateAuthorizationURL(
                // Is this safe? Yes, you could change the query param anyway and oauth prevents that
                "${request.scheme()}://${request.host()}/callback",
                Scope.IDENTIFY, Scope.GUILDS
            )

            val debug = request.queryParamOrDefault("debug", null)

            if (!debug.isNullOrBlank()) {
                url = url.replace("discord.com", "$debug.discord.com")
                    .replace("/api/v$DISCORD_REST_VERSION", "")
            }

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
