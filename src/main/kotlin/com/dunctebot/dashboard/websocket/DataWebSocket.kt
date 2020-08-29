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
import org.eclipse.jetty.websocket.api.Session
import org.eclipse.jetty.websocket.api.StatusCode
import org.eclipse.jetty.websocket.api.annotations.*
import java.io.IOException
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

@WebSocket
class DataWebSocket {
    // Store sessions if you want to, for example, broadcast a message to all users
    private val sessions: Queue<Session> = ConcurrentLinkedQueue()

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

        session.remote.sendString("""{"t": "ROLES_PUT_HASH"}""")
    }

    @OnWebSocketClose
    fun onClosed(session: Session, statusCode: Int, reason: String?) {
        sessions.remove(session)
    }

    @OnWebSocketMessage
    @Throws(IOException::class)
    fun onMessage(session: Session, message: String) {
        println("Got: $message")
    }

    @OnWebSocketError
    fun onError(session: Session, thr: Throwable) {
        thr.printStackTrace()
    }
}
