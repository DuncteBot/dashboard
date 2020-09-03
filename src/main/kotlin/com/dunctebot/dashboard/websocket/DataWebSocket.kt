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
import com.dunctebot.dashboard.jsonMapper
import com.dunctebot.dashboard.utils.HashUtils
import com.dunctebot.dashboard.websocket.handlers.DataUpdateHandler
import com.dunctebot.dashboard.websocket.handlers.FetchDataHandler
import com.dunctebot.dashboard.websocket.handlers.RolesHashHandler
import com.dunctebot.dashboard.websocket.handlers.base.SocketHandler
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import org.eclipse.jetty.websocket.api.Session
import org.eclipse.jetty.websocket.api.StatusCode
import org.eclipse.jetty.websocket.api.annotations.*
import org.slf4j.LoggerFactory
import java.io.IOException
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors

@WebSocket
class DataWebSocket {
    private val logger = LoggerFactory.getLogger(DataWebSocket::class.java)

    // Store sessions if you want to, for example, broadcast a message to all users
    private val sessions: Queue<Session> = ConcurrentLinkedQueue()
    private val handlersMap = mutableMapOf<String, SocketHandler>()

    private val executor = Executors.newSingleThreadExecutor {
        val t = Thread(it, "WS-SendThread")
        t.isDaemon = true
        return@newSingleThreadExecutor t
    }

    init {
        setupHandlers()
    }

    @OnWebSocketConnect
    fun onConnected(session: Session) {
        val token: String? = session.upgradeRequest.getHeader("Authorization")

        if (token.isNullOrBlank()) {
            session.close(StatusCode.POLICY_VIOLATION, "Unauthorised")
            return
        }

        if (!duncteApis.validateToken(token)) {
            session.close(StatusCode.POLICY_VIOLATION, "Unauthorised")
            return
        }

        sessions.add(session)
    }

    @OnWebSocketClose
    fun onClosed(session: Session, statusCode: Int, reason: String?) {
        sessions.remove(session)
        println("Client ${session.remoteAddress} disconnected with code $statusCode and reason $reason")
    }

    @OnWebSocketMessage
    @Throws(IOException::class)
    fun onMessage(session: Session, message: String) {
        println("Got: $message")

        try {
            val json = jsonMapper.readTree(message)

            if (json.has("t")) {
                dispatch(session, json)
            }
        } catch (e: JsonProcessingException) {
            logger.error("Error when parsing json from WS", e)
        }
    }

    @OnWebSocketError
    fun onError(session: Session, thr: Throwable) {
        thr.printStackTrace()
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
                println("Broadcasting $message")
                sessions.forEach {
                    it.remote.sendString(
                        jsonMapper.writeValueAsString(message)
                    )
                }
            } catch (e: Exception) {
                logger.error("Error with broadcast", e)
            }
        }
    }

    private fun dispatch(session: Session, raw: JsonNode) {
        val type = raw["t"].asText()
        val handler = handlersMap[type]

        if (handler == null) {
            logger.warn("Unknown event or missing handler for type $type")
            return
        }

        handler.handle(session, raw)
    }

    private fun setupHandlers() {
        handlersMap["ROLES_PUT_HASH"] = RolesHashHandler()
        handlersMap["DATA_UPDATE"] = DataUpdateHandler()
        handlersMap["FETCH_DATA"] = FetchDataHandler()
    }
}
