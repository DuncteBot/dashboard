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

import net.dv8tion.jda.internal.JDAImpl
import net.dv8tion.jda.internal.utils.config.AuthorizationConfig
import net.dv8tion.jda.internal.utils.config.MetaConfig
import net.dv8tion.jda.internal.utils.config.SessionConfig
import net.dv8tion.jda.internal.utils.config.ThreadingConfig

/**
 * Custom jda rest client that allows for rest-only usage of JDA
 *
 * This class has been inspired by GivawayBot and all credit goes to them https://github.com/jagrosh/GiveawayBot
 */
class JDARestClient {
    private val jda: JDAImpl

    constructor(token: String) {
        val authConfig = AuthorizationConfig(token)
        val sessionConfig = SessionConfig.getDefault()
        val threadConfig = ThreadingConfig.getDefault()
        val metaConfig = MetaConfig.getDefault()

        jda = JDAImpl(authConfig, sessionConfig, threadConfig, metaConfig)
    }

}
