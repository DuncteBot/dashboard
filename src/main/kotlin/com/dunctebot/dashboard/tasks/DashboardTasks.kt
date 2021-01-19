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

package com.dunctebot.dashboard.tasks

import com.dunctebot.dashboard.controllers.GuildController
import com.dunctebot.dashboard.controllers.api.OtherAPi
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class DashboardTasks {
    private val threadPool = Executors.newScheduledThreadPool(4) {
        val t = Thread(it, "DashboardTasksThread")
        t.isDaemon = true
        return@newScheduledThreadPool t
    }

    init {
        // start cleaners
        // clean the hashes pool every hour
        threadPool.scheduleAtFixedRate(
            GuildController.guildHashes::cleanUp,
            1,
            1,
            TimeUnit.HOURS
        )
        threadPool.scheduleAtFixedRate(
            GuildController.guildRoleCache::cleanUp,
            1,
            1,
            TimeUnit.HOURS
        )
        // Clean the guilds pool every 30 minutes
        threadPool.scheduleAtFixedRate(
            OtherAPi.guildsRequests::cleanUp,
            30,
            30,
            TimeUnit.MINUTES
        )
        // Clean the security keys on a daily basis
        threadPool.scheduleAtFixedRate(
            GuildController.securityKeys::clear,
            1,
            1,
            TimeUnit.DAYS
        )
    }
}
