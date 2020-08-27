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

package com.dunctebot.dashboard

import io.github.cdimascio.dotenv.Dotenv
import spark.Spark.*

class Server(private val env: Dotenv) {

    init {
        port(env["SERVER_PORT"]!!.toInt())
        ipAddress(env["SERVER_IP"])

        get("/") {_, _ ->
            val user = jda.retrieveUserById("191231307290771456").complete()

            "Hello World" +
                "<h1>Kotlin ${KotlinVersion.CURRENT}</h1>" +
                "<h1>Spark 2.9.1</h1>" +
                "<h1>User with ID:191231307290771456 is: ${user.asTag}</h1>"
        }
    }

    fun shutdown() {
        awaitStop()
    }
}
