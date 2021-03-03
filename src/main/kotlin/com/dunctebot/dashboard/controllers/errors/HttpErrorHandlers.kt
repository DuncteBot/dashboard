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
                .put("hide_menu", true)
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
                .put("hide_menu", true)
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
