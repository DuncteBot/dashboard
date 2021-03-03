package com.dunctebot.dashboard.controllers.api

import com.dunctebot.dashboard.authOrFail
import com.dunctebot.dashboard.duncteApis
import com.dunctebot.dashboard.jsonMapper
import com.dunctebot.dashboard.webSocket
import com.fasterxml.jackson.databind.node.ObjectNode
import spark.Request

object DataController {
    fun updateData(request: Request): Any {
        request.authOrFail()

        // parse the data to make sure that it is proper json
        val updateData = jsonMapper.readTree(request.bodyAsBytes())
        val wsRequest = jsonMapper.createObjectNode()
            .put("t", "DATA_UPDATE")
            .set<ObjectNode>("d", updateData)

        // send the data to all instances that are connected
        webSocket.broadcast(wsRequest)

        return jsonMapper.createObjectNode().put("success", true)
    }

    fun invalidateTokens(request: Request): Any {
        request.authOrFail()

        duncteApis.validTokens.clear()

        return "ok"
    }
}
