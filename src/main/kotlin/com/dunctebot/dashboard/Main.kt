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

import com.dunctebot.dashboard.controllers.GuildController
import com.dunctebot.dashboard.controllers.api.OtherAPi
import com.dunctebot.duncteapi.DuncteApi
import com.dunctebot.jda.JDARestClient
import io.github.cdimascio.dotenv.dotenv
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

lateinit var restJDA: JDARestClient
lateinit var duncteApis: DuncteApi

private val systemPool = Executors.newScheduledThreadPool(4) { Thread(it, "Bot-Service-Thread") }

fun main() {
    val logger = LoggerFactory.getLogger("Main")
    val env = dotenv()

    restJDA = JDARestClient(env["BOT_TOKEN"]!!)
    duncteApis = DuncteApi(env["BOT_TOKEN"]!!)

    Server(env)

    // start cleaners
    // clean the hashes pool every hour
    systemPool.scheduleAtFixedRate(
        GuildController.guildHashes::cleanUp,
        1,
        1,
        TimeUnit.HOURS
    )
    // Clean the guilds pool every 30 minutes
    systemPool.scheduleAtFixedRate(
        OtherAPi.guildsRequests::cleanUp,
        30,
        30,
        TimeUnit.MINUTES
    )

    logger.info("Application ready: http://{}:{}/", env["SERVER_IP"], env["SERVER_PORT"])
}
