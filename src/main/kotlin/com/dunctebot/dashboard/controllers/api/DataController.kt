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

package com.dunctebot.dashboard.controllers.api

import com.dunctebot.dashboard.duncteApis
import com.dunctebot.dashboard.jsonMapper
import com.dunctebot.dashboard.webSocket
import spark.Request
import spark.Spark

object DataController {
    fun updateData(request: Request): Any {
        if (!request.headers().contains("Authorization") || !duncteApis.validateToken(request.headers("Authorization"))) {
            Spark.halt(401)
        }

        // parse the data to make sure that it is proper json
        val updateData = jsonMapper.readTree(request.bodyAsBytes())

        // send the data to all instances that are connected
        webSocket.broadcast(updateData)

        return jsonMapper.createObjectNode().put("success", true)
    }
}
