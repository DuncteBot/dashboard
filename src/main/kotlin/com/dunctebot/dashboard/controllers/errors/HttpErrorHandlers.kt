/*
 * Skybot, a multipurpose discord bot
 *      Copyright (C) 2017 - 2020  Duncan "duncte123" Sterken & Ramid "ramidzkh" Khan & Maurice R S "Sanduhr32"
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.dunctebot.dashboard.controllers.errors

import com.dunctebot.dashboard.constants.ContentType
import com.dunctebot.dashboard.jsonMapper
import com.dunctebot.dashboard.rendering.WebVariables
import spark.Request
import spark.Response

object HttpErrorHandlers {

    fun notFound(request: Request, response: Response): Any {
        if (request.headers("Accept") != ContentType.JSON || response.type() != ContentType.JSON) {
            response.type(ContentType.HTML)

            return WebVariables()
                .put("title", "404 - Page Not Found")
                .toModelAndView("errors/404.vm")
        }

        response.type(ContentType.JSON)

        return jsonMapper.createObjectNode()
            .put("success", false)
            .put("message", "'${request.pathInfo()}' was not found")
            .put("code", response.status())
    }

    fun internalServerError(request: Request, response: Response): Any {
        if (request.headers("Accept") != ContentType.JSON || response.type() != ContentType.JSON) {
            response.type(ContentType.HTML)

            return WebVariables()
                .put("title", "500 - Internal Server error")
                .toModelAndView("errors/500.vm")
        }

        response.type(ContentType.JSON)

        return jsonMapper.createObjectNode()
            .put("success", false)
            .put("message", "Internal server error")
            .put("code", response.status())
    }

}
