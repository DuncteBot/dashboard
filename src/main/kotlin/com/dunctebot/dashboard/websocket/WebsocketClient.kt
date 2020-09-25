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

package com.dunctebot.dashboard.websocket

import com.dunctebot.dashboard.duncteApis
import com.dunctebot.dashboard.env
import com.dunctebot.dashboard.jsonMapper
import com.dunctebot.dashboard.tasks.ReconnectTask
import com.dunctebot.dashboard.utils.HashUtils
import com.dunctebot.dashboard.websocket.handlers.DataUpdateHandler
import com.dunctebot.dashboard.websocket.handlers.FetchDataHandler
import com.dunctebot.dashboard.websocket.handlers.RolesHashHandler
import com.dunctebot.dashboard.websocket.handlers.base.SocketHandler
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.neovisionaries.ws.client.*
import net.dv8tion.jda.internal.utils.IOUtil
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class WebsocketClient : WebSocketAdapter(), WebSocketListener {
    private val logger = LoggerFactory.getLogger(WebsocketClient::class.java)
    private val handlersMap = mutableMapOf<String, SocketHandler>()

    private val executor = Executors.newSingleThreadExecutor {
        val t = Thread(it, "WS-SendThread")
        t.isDaemon = true
        return@newSingleThreadExecutor t
    }

    private val reconnectThread = Executors.newSingleThreadScheduledExecutor {
        val t = Thread(it, "WS-ReconnectThread")
        t.isDaemon = true
        return@newSingleThreadScheduledExecutor t
    }

    private val factory = WebSocketFactory()
        .setConnectionTimeout(10000)
        .setServerName(IOUtil.getHost(env["WS_URL"]))
    lateinit var socket: WebSocket

    var mayReconnect = true
    var lastReconnectAttempt = 0L
    var reconnectsAttempted = 0
    val reconnectInterval: Int
        get() = reconnectsAttempted * 2000 - 2000

    init {
        setupHandlers()
        connect()

        reconnectThread.scheduleWithFixedDelay(
            ReconnectTask(this),
            0L,
            500L,
            TimeUnit.MILLISECONDS
        )
    }

    override fun onConnected(websocket: WebSocket, headers: MutableMap<String, MutableList<String>>) {
        logger.info("Connected to dashboard WebSocket")

        socket.sendText("""{"t": "test"}""")
    }

    override fun onDisconnected(websocket: WebSocket, serverCloseFrame: WebSocketFrame?, clientCloseFrame: WebSocketFrame?, closedByServer: Boolean) {
        if (closedByServer && serverCloseFrame != null) {
            val reason = serverCloseFrame.closeReason ?: "<no reason given>"
            val code = serverCloseFrame.closeCode

            // TODO: keep trying to reconnect?
            if (code == 1000) {
                reconnectThread.shutdown()
                mayReconnect = false
                logger.info("Connection to {} closed normally with reason {} (closed by server = true)", websocket.uri, reason)
            } else {
                logger.info("Connection to {} closed abnormally with reason {}:{} (closed by server = true)", websocket.uri, code, reason)
            }

            return
        } else if (clientCloseFrame != null) {
            val code = clientCloseFrame.closeCode
            val reason = clientCloseFrame.closeReason ?: "<no reason given>"

            // 1000 is normal
            if (code == 1000) {
                reconnectThread.shutdown()
                mayReconnect = false
            }

            logger.info("Connection to {} closed by client with code {} and reason {} (closed by server = false)", websocket.uri, code, reason)
        }
    }

    override fun onTextMessage(websocket: WebSocket, text: String) {
        println("message: $text")
    }

    override fun onTextMessage(websocket: WebSocket, data: ByteArray) {
        val raw = jsonMapper.readTree(data)

        if (!raw.has("t")) {
            return
        }

        val type = raw["t"].asText()
        val handler = handlersMap[type]

        if (handler == null) {
            logger.warn("Unknown event or missing handler for type $type")
            return
        }

        handler.handle(raw)
    }

    fun requestData(data: JsonNode, callback: (JsonNode) -> Unit) {
        val hash = HashUtils.sha1(data.toString() + System.currentTimeMillis())

        (data as ObjectNode).put("identifier", hash)

        val request = jsonMapper.createObjectNode()
            .put("t", "FETCH_DATA")
            .set<ObjectNode>("d", data)

        (handlersMap["FETCH_DATA"]!! as FetchDataHandler).waitingMap[hash] = callback

        broadcast(request)
    }

    fun broadcast(message: JsonNode) {
        executor.submit {
            try {
                logger.debug("-> {}", message)

                socket.sendText(jsonMapper.writeValueAsString(message))
            } catch (e: Exception) {
                logger.error("Error with broadcast", e)
            }
        }
    }

    private fun setupHandlers() {
        handlersMap["ROLES_PUT_HASH"] = RolesHashHandler()
        handlersMap["DATA_UPDATE"] = DataUpdateHandler()
        handlersMap["FETCH_DATA"] = FetchDataHandler()
    }

    fun attemptReconnect() {
        lastReconnectAttempt = System.currentTimeMillis()
        reconnectsAttempted++
        connect()
    }

    private fun connect() {
        socket = factory.createSocket(env["WS_URL"])

        socket.setDirectTextMessage(true)
            .addHeader("X-DuncteBot", "dashboard")
            .addHeader("Accept-Encoding", "gzip")
            .addHeader("Authorization", duncteApis.apiKey)
            .addListener(this)

        try {
            socket.connect()
            // reset reconnects after successful connect
            reconnectsAttempted = 0
        } catch (e: Exception) {
            logger.error("Failed to connect to WS, retrying in ${reconnectInterval / 1000} seconds", e)
        }
    }
}
