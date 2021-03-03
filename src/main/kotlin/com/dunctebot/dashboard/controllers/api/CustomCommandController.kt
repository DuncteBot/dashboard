package com.dunctebot.dashboard.controllers.api

import com.dunctebot.dashboard.WebServer.Companion.SESSION_ID
import com.dunctebot.dashboard.WebServer.Companion.USER_ID
import com.dunctebot.dashboard.constants.ContentType
import com.dunctebot.dashboard.duncteApis
import com.dunctebot.dashboard.guildId
import com.dunctebot.dashboard.jsonMapper
import com.dunctebot.dashboard.webSocket
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import spark.Request
import spark.Response
import spark.Spark

object CustomCommandController {
    fun before(request: Request, response: Response) {
        val attributes = request.session().attributes()

        if (!(attributes.contains(USER_ID) && attributes.contains(SESSION_ID))) {
            response.type(ContentType.JSON)

            Spark.halt(401,
                jsonMapper.writeValueAsString(
                    jsonMapper.createObjectNode()
                        .put("status", "error")
                        .put("message", "Invalid session")
                        .put("code", response.status())
                )
            )
        }
    }

    fun show(request: Request, response: Response): Any {
        val commands = duncteApis.fetchCustomCommands(request.guildId!!.toLong()) as ArrayNode

        val res = jsonMapper.createObjectNode()
            .put("success", true)
            .put("code", response.status())
        val arr = res.putArray("commands")

        arr.addAll(commands)

        return res
    }

    fun update(request: Request, response: Response): Any {
        val commandData = jsonMapper.readTree(request.bodyAsBytes())

        if (!commandData.has("invoke") || !commandData.has("message")) {
            response.status(403)

            return jsonMapper.createObjectNode()
                .put("success", false)
                .put("message", "Invalid data")
                .put("code", response.status())
        }

        val invoke = commandData["invoke"].asText()
        val message = commandData["message"].asText()
        val autoresponse = commandData["autoresponse"].asBoolean(false)

        val guildId = request.guildId!!.toLong()

        // TODO: this should return a 404 error if the command does not exist
        //  (saves a request to the database)
        val returnData = duncteApis.updateCustomCommand(guildId, invoke, message, autoresponse).first

        if (!returnData) {
            return jsonMapper.createObjectNode()
                .put("success", false)
                .put("message", "Could not update command")
                .put("code", response.status())
        }

        val updateData = jsonMapper.createObjectNode()
        updateData.putArray("update")
            .add(
                jsonMapper.createObjectNode()
                    .put("guild_id", guildId)
                    .put("invoke", invoke)
                    .put("message", message)
                    .put("autoresponse", autoresponse)
            )

        sendCustomCommandUpdate(updateData)

        return jsonMapper.createObjectNode()
            .put("success", true)
            .put("code", response.status())
    }

    fun create(request: Request, response: Response): Any {
        val commandData = jsonMapper.readTree(request.bodyAsBytes())

        if (!commandData.has("invoke") || !commandData.has("message") || !commandData.has("autoresponse")) {
            response.status(403)

            return jsonMapper.createObjectNode()
                .put("success", false)
                .put("message", "Invalid data")
                .put("code", response.status())
        }

        val invoke = commandData["invoke"].asText().replace("\\s".toRegex(), "")

        if (invoke.length > 25) {
            return jsonMapper.createObjectNode()
                .put("success", false)
                .put("message", "Invoke is over 25 characters")
                .put("code", response.status())
        }

        val message = commandData["message"].asText()

        if (message.length > 4000) {
            return jsonMapper.createObjectNode()
                .put("success", false)
                .put("message", "Message is over 4000 characters")
                .put("code", response.status())
        }

        val guildId = request.guildId!!.toLong()

        if (duncteApis.fetchCustomCommand(guildId, invoke) != null) {
            response.status(404)

            return jsonMapper.createObjectNode()
                .put("success", false)
                .put("message", "Command already exists")
                .put("code", response.status())
        }

        val autoresponse = commandData["autoresponse"].asBoolean(false)

        val result = duncteApis.createCustomCommand(guildId, invoke, message, autoresponse)

        if (result.first) {
            val updateData = jsonMapper.createObjectNode()
            updateData.putArray("add")
                .add(
                    jsonMapper.createObjectNode()
                        .put("guild_id", guildId)
                        .put("invoke", invoke)
                        .put("message", message)
                        .put("autoresponse", autoresponse)
                )

            sendCustomCommandUpdate(updateData)

            return jsonMapper.createObjectNode()
                .put("success", true)
                .put("message", "Command added")
                .put("code", response.status())
        }

        if (result.second) {
            return jsonMapper.createObjectNode()
                .put("success", false)
                .put("message", "Command already exists")
                .put("code", response.status())
        }

        if (result.third) {
            return jsonMapper.createObjectNode()
                .put("success", false)
                .put("message", "You reached the limit of 50 custom commands for this server")
                .put("code", response.status())
        }

        return jsonMapper.createObjectNode()
            .put("success", false)
            .put("message", "Database error")
            .put("code", response.status())
    }

    fun delete(request: Request, response: Response): Any {
        val commandData = jsonMapper.readTree(request.bodyAsBytes())

        if (!commandData.has("invoke")) {
            response.status(403)

            return jsonMapper.createObjectNode()
                .put("success", false)
                .put("message", "Invalid data")
                .put("code", response.status())
        }

        val invoke = commandData["invoke"].asText()

        val guildId = request.guildId!!.toLong()

        if (duncteApis.fetchCustomCommand(guildId, invoke) == null) {
            response.status(404)

            return jsonMapper.createObjectNode()
                .put("success", false)
                .put("message", "Command does not exists")
                .put("code", response.status())
        }

        val success = duncteApis.deleteCustomCommand(guildId, invoke).first

        if (!success) {
            return jsonMapper.createObjectNode()
                .put("success", false)
                .put("message", "Could not delete command")
                .put("code", response.status())
        }

        val updateData = jsonMapper.createObjectNode()
        updateData.putArray("remove")
            .add(
                jsonMapper.createObjectNode()
                    .put("guild_id", guildId)
                    .put("invoke", invoke)
            )

        sendCustomCommandUpdate(updateData)

        return jsonMapper.createObjectNode()
            .put("success", true)
            .put("message", "Command deleted")
            .put("code", response.status())
    }

    private fun sendCustomCommandUpdate(data: JsonNode) {
        val json = jsonMapper.createObjectNode()
            .put("t", "CUSTOM_COMMANDS")
            .set<ObjectNode>("d", data)

        webSocket.broadcast(json)
    }
}
