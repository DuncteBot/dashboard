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

package com.dunctebot.discord.api.entities

import com.dunctebot.discord.api.DiscordClient

interface User : ISnowflake {
    val name: String

    val discriminator: String

    val avatarId: String?

    val avatarUrl: String?
        get() = if (avatarId == null) null else {
            val extension = if (avatarId!!.startsWith("a_")) "gif" else "png"
            AVATAR_URL.format(id, avatarId, extension)
        }

    val defaultAvatarId: String

    val defaultAvatarUrl: String
        get() = DEFAULT_AVATAR_URL.format(id, defaultAvatarId)

    val effectiveAvatarUrl: String
        get() = if (avatarId == null) defaultAvatarUrl else avatarUrl!!

    val tag: String

    val hasPrivateChannel: Boolean

    // todo: open private channel

    // todo: get mutual guilds

    val bot: Boolean

    val client: DiscordClient

    // todo: flags

    val flagsRaw: Int

    companion object {
        const val AVATAR_URL = "https://cdn.discordapp.com/avatars/%s/%s.%s"
        const val DEFAULT_AVATAR_URL = "https://cdn.discordapp.com/embed/avatars/%s.png"
    }
}
