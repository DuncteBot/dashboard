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
import com.dunctebot.dashboard.utils.fetchGuildPatronStatus
import com.dunctebot.models.settings.GuildSetting
import com.dunctebot.models.settings.WarnAction
import com.dunctebot.models.utils.Utils.colorToInt
import net.dv8tion.jda.api.entities.Guild
import spark.Request
import spark.Response
import spark.Spark
import kotlin.math.max
import kotlin.math.min

object SettingsController {
    fun saveBasic(request: Request, response: Response): Any {
        val params = request.paramsMap

        var prefix = params["prefix"] ?: "db!"

        if (prefix.length > 10) {
            prefix = prefix.substring(0, 10)
        }

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
            .setAutoroleRole(autorole)
            .setAnnounceTracks(announceTracks)
            .setLeaveTimeout(leaveTimeout)
            .setAllowAllToStop(allowAllToStop)
            .setEmbedColor(color)

        sendSettingUpdate(settings)

        request.session().attribute(FLASH_MESSAGE, "<h4>Settings updated</h4>")

        return response.redirect(request.url())
    }

    fun saveModeration(request: Request, response: Response): Any {
        val params = request.paramsMap

        val modLogChannel = params["modChannel"].toSafeLong()
        val autoDeHoist = params["autoDeHoist"].toCBBool()
        val filterInvites = params["filterInvites"].toCBBool()
        val swearFilter = params["swearFilter"].toCBBool()
        val muteRole = params["muteRole"].toSafeLong()
        val spamFilter = params["spamFilter"].toCBBool()
        val kickMode = params["kickMode"].toCBBool()
        val spamThreshold = params["spamThreshold"]?.toIntOrNull() ?: 7
        val filterType = params["filterType"]

        val logBan = params["logBan"].toCBBool()
        val logUnban = params["logUnban"].toCBBool()
        val logMute = params["logMute"].toCBBool()
        val logKick = params["logKick"].toCBBool()
        val logWarn = params["logWarn"].toCBBool()
        val logInvite = params["logInvite"].toCBBool()

        val aiSensitivity = ((params["ai-sensitivity"] ?: "0.7").toFloatOrNull() ?: 0.7f).minMax(0f, 1f)
        val rateLimits = parseRateLimits(request, params) ?: return response.redirect(request.url())

        val youngAccountThreshold = params["young_account_threshold"]?.toIntOrNull() ?: 10
        val youngAccountBanEnable = params["young_account_ban_enabled"].toCBBool()

        val guild = request.fetchGuild()!!
        val guildId = guild.idLong
        val warnActionsList = parseWarnActions(guild, params)

        val settings = duncteApis.getGuildSetting(request.guildId!!.toLong())
            .setLogChannel(modLogChannel)
            .setAutoDeHoist(autoDeHoist)
            .setFilterInvites(filterInvites)
            .setMuteRoleId(muteRole)
            .setKickState(kickMode)
            .setRatelimits(rateLimits)
            .setEnableSpamFilter(spamFilter)
            .setEnableSwearFilter(swearFilter)
            .setSpamThreshold(spamThreshold)
            .setFilterType(filterType)
            .setBanLogging(logBan)
            .setUnbanLogging(logUnban)
            .setMuteLogging(logMute)
            .setKickLogging(logKick)
            .setWarnLogging(logWarn)
            .setInviteLogging(logInvite)
            .setAiSensitivity(aiSensitivity)
            .setWarnActions(warnActionsList)
            .setYoungAccountThreshold(youngAccountThreshold)
            .setYoungAccountBanEnabled(youngAccountBanEnable)

        sendSettingUpdate(settings)

        duncteApis.updateWarnActions(guildId, settings.warnActions)

        request.session().attribute(FLASH_MESSAGE, "<h4>Settings updated</h4>")

        return response.redirect(request.url())
    }

    fun saveMessages(request: Request, response: Response): Any {
        val params = request.paramsMap

        val welcomeEnabled = params["welcomeChannelCB"].toCBBool()
        val leaveEnabled = params["leaveChannelCB"].toCBBool()
        val welcomeMessage = params["welcomeMessage"]
        val leaveMessage = params["leaveMessage"]
        val serverDescription = params["serverDescription"]
        val welcomeChannel = params["welcomeChannel"].toSafeLong()

        val settings = duncteApis.getGuildSetting(request.guildId!!.toLong())
            .setServerDesc(serverDescription)
            .setWelcomeLeaveChannel(welcomeChannel)
            .setCustomJoinMessage(welcomeMessage)
            .setCustomLeaveMessage(leaveMessage)
            .setEnableJoinMessage(welcomeEnabled)
            .setEnableLeaveMessage(leaveEnabled)

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

    private fun parseRateLimits(request: Request, params: Map<String, String>): LongArray? {
        val rateLimits = LongArray(6)

        for (i in 0..5) {
            val reqItemId = i + 1
            val value = params.getValue("rateLimits[$reqItemId]")

            if (value.isEmpty()) {
                request.session().attribute(FLASH_MESSAGE, "<h4>Rate limits are invalid</h4>")

                return null
            }

            rateLimits[i] = value.toLong()
        }

        return rateLimits
    }

    private fun parseWarnActions(guild: Guild, params: Map<String, String>): List<WarnAction> {
        val warnActionsList = arrayListOf<WarnAction>()
        val isGuildPatron = fetchGuildPatronStatus(guild.id)
        val maxWarningActionCount = if(isGuildPatron) WarnAction.PATRON_MAX_ACTIONS else 1


        for (i in 1 until maxWarningActionCount + 1) {
            if (!params.containsKey("warningAction$i")) {
                continue
            }

            if (!params.containsKey("tempDays$i") ||
                !params.containsKey("threshold$i")
            ) {
                Spark.halt(400, "Invalid warn action detected")
            }

            if (
            // Check for empty values (they should never be empty)
                params["warningAction$i"].isNullOrEmpty() ||
                params["tempDays$i"].isNullOrEmpty() ||
                params["threshold$i"].isNullOrEmpty()
            ) {
                Spark.halt(400, "One or more warn actions has empty values")
            }

            val action = WarnAction.Type.valueOf(params.getValue("warningAction$i"))
            val duration = params.getValue("tempDays$i").toInt()
            val threshold = params.getValue("threshold$i").toInt()

            warnActionsList.add(WarnAction(action, threshold, duration))
        }

        return warnActionsList
    }

    private fun Float.minMax(min: Float, max: Float): Float {
        // max returns the highest value and min returns the lowest value
        return min(max, max(min, this))
    }
}
