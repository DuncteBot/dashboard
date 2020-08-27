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

package com.dunctebot.jda

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.utils.SessionController
import net.dv8tion.jda.internal.utils.tuple.Pair

class FakeSessionController : SessionController {
    private var globalRateLimit = 0L

    override fun appendSession(node: SessionController.SessionConnectNode) {
        // do nothing
    }

    override fun removeSession(node: SessionController.SessionConnectNode) {
        TODO("Not yet implemented")
    }

    override fun getGlobalRatelimit(): Long  = globalRateLimit

    override fun setGlobalRatelimit(ratelimit: Long) {
        globalRateLimit = ratelimit
    }

    override fun getGateway(api: JDA): String {
        TODO("Not yet implemented")
    }

    override fun getGatewayBot(api: JDA): Pair<String, Int> {
        TODO("Not yet implemented")
    }
}
