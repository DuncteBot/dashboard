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

package com.dunctebot.dashboard.controllers

import com.dunctebot.dashboard.*
import com.dunctebot.dashboard.WebServer.Companion.FLASH_MESSAGE
import com.dunctebot.models.settings.GuildSetting
import com.dunctebot.models.utils.Utils.colorToInt
import spark.Request
import spark.Response

object SettingsController {
    fun saveBasic(request: Request, response: Response): Any {
        val params = request.paramsMap

        var prefix = params["prefix"] ?: "db!"

        if (prefix.length > 10) {
            prefix = prefix.substring(0, 10)
        }

        val welcomeChannel = params["welcomeChannel"].toSafeLong()
        val welcomeLeaveEnabled = params["welcomeChannelCB"].toCBBool()
        val autorole = params["autoRoleRole"].toSafeLong()
        val announceTracks = params["announceTracks"].toCBBool()
        val allowAllToStop = params["allowAllToStop"].toCBBool()
        val color = colorToInt(params["embedColor"])
        var leaveTimeout = params["leaveTimeout"].toSafeLong().toInt()

        if (leaveTimeout < 1 || leaveTimeout > 60) {
            leaveTimeout = 1
        }

        // never really over

        val settings = duncteApis.getGuildSetting(request.guildId!!.toLong())
            .setCustomPrefix(prefix)
            .setWelcomeLeaveChannel(welcomeChannel)
            .setEnableJoinMessage(welcomeLeaveEnabled)
            .setAutoroleRole(autorole)
            .setAnnounceTracks(announceTracks)
            .setLeaveTimeout(leaveTimeout)
            .setAllowAllToStop(allowAllToStop)
            .setEmbedColor(color)

        sendSettingUpdate(settings)

        request.session().attribute(FLASH_MESSAGE, "<h4>Settings updated</h4>")

        return response.redirect(request.url())
    }


    private fun sendSettingUpdate(setting: GuildSetting) {
        val request = jsonMapper.createObjectNode()
            .put("t", "GUILD_SETTINGS")

        request.putObject("d")
            .putArray("update")
            .add(setting.toJson(jsonMapper))

        webSocket.broadcast(request)

        duncteApis.saveGuildSetting(setting)
    }
}
