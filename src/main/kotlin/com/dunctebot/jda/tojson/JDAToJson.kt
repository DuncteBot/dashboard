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

package com.dunctebot.jda.tojson

import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.utils.data.DataArray
import net.dv8tion.jda.api.utils.data.DataObject

fun Guild.toJsonWithChannels(): DataObject {
    val mainJson = this.toJson()

    val textChannelCache = this.textChannelCache

    if (!textChannelCache.isEmpty) {
        val channelsArray = DataArray.empty()

        textChannelCache.forEach {
            channelsArray.add(it.toJson(false))
        }

        mainJson.put("text_channels", channelsArray)
    }

    val voiceChannelCache = this.voiceChannelCache

    if (!voiceChannelCache.isEmpty) {
        val channelsArray = DataArray.empty()

        voiceChannelCache.forEach {
            channelsArray.add(it.toJson(false))
        }

        mainJson.put("voice_channels", channelsArray)
    }

    val roleCache = this.roleCache

    if (!roleCache.isEmpty) {
        val rolesArray = DataArray.empty()

        roleCache.forEach {
            rolesArray.add(it.toJson(false))
        }

        mainJson.put("roles", rolesArray)
    }

    return mainJson
}

fun Guild.toJson(): DataObject {
    val json = DataObject.empty()

    json.put("id", this.id)
        .put("name", this.name)
        .put("icon", this.iconId)
        .put("splash", this.splashId)
        .put("discovery_splash", null)
        .put("owner", false)
        .put("owner_id", this.ownerId)
        .put("region", this.regionRaw)
        .put("afk_channel_id", this.afkChannel?.id)
        .put("afk_timeout", this.afkTimeout.seconds)
        .put("verification_level", this.verificationLevel.key)
        .put("default_message_notifications", this.defaultNotificationLevel.key)
        .put("explicit_content_filter", this.explicitContentLevel.key)
        .put("roles", this.roleCache.map { it.toJson(false) })
        .put("emojis", this.emoteCache.map { it.toJson(false) })
        .put("features", this.features)
        .put("mfa_level", this.requiredMFALevel.key)

    return json
}

fun Emote.toJson(withGuild: Boolean = true): DataObject {
    return DataObject.empty()
}

fun Role.toJson(withGuild: Boolean = true): DataObject {
    return DataObject.empty()
}

fun TextChannel.toJson(withGuild: Boolean = true): DataObject {
    return DataObject.empty()
}

fun VoiceChannel.toJson(withGuild: Boolean = true): DataObject {
    return DataObject.empty()
}
