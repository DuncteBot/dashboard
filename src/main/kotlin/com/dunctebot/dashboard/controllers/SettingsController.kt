package com.dunctebot.dashboard.controllers

import com.dunctebot.dashboard.*
import com.dunctebot.dashboard.WebServer.Companion.FLASH_MESSAGE
import com.dunctebot.dashboard.utils.fetchGuildPatronStatus
import com.dunctebot.models.settings.GuildSetting
import com.dunctebot.models.settings.WarnAction
import com.dunctebot.models.utils.Utils.colorToInt
import spark.Request
import spark.Response
import spark.Spark
import kotlin.math.max
import kotlin.math.min

object SettingsController {
    fun saveSettings(request: Request, response: Response): Any {
        val params = request.paramsMap
        val rateLimits = parseRateLimits(params)

        // rate limits are null when parsing failed
        if (rateLimits == null) {
            request.session().attribute(FLASH_MESSAGE, "<h4>Rate limits are invalid</h4>")

            return response.redirect(request.url())
        }

        val settings = duncteApis.getGuildSetting(request.guildId!!.toLong())

        setBasic(params, settings)
        setModeration(params, rateLimits, settings)
        setMessages(params, settings)

        sendSettingUpdate(settings)

        request.session().attribute(FLASH_MESSAGE, "All settings updated!")

        return response.redirect(request.url())
    }

    private fun setBasic(params: Map<String, String>, settings: GuildSetting) {
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
            settings.setCustomPrefix(prefix)
            .setAutoroleRole(autorole)
            .setAnnounceTracks(announceTracks)
            .setLeaveTimeout(leaveTimeout)
            .setAllowAllToStop(allowAllToStop)
            .setEmbedColor(color)
    }

    private fun setModeration(params: Map<String, String>, rateLimits: LongArray, settings: GuildSetting) {
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

        val youngAccountThreshold = params["young_account_threshold"]?.toIntOrNull() ?: 10
        val youngAccountBanEnable = params["young_account_ban_enabled"].toCBBool()

        val guildId = settings.guildId
        val warnActionsList = parseWarnActions(guildId, params)

        settings
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
    }

    private fun setMessages(params: Map<String, String>, settings: GuildSetting) {
        val welcomeEnabled = params["welcomeChannelCB"].toCBBool()
        val leaveEnabled = params["leaveChannelCB"].toCBBool()
        val welcomeMessage = params["welcomeMessage"]
        val leaveMessage = params["leaveMessage"]
        val serverDescription = params["serverDescription"]
        val welcomeChannel = params["welcomeChannel"].toSafeLong()

        settings.setServerDesc(serverDescription)
            .setWelcomeLeaveChannel(welcomeChannel)
            .setCustomJoinMessage(welcomeMessage)
            .setCustomLeaveMessage(leaveMessage)
            .setEnableJoinMessage(welcomeEnabled)
            .setEnableLeaveMessage(leaveEnabled)
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

    private fun parseRateLimits(params: Map<String, String>): LongArray? {
        val rateLimits = LongArray(6)

        for (i in 0..5) {
            val reqItemId = i + 1
            val value = params.getValue("rateLimits[$reqItemId]")

            if (value.isEmpty()) {
                return null
            }

            rateLimits[i] = value.toLong()
        }

        return rateLimits
    }

    private fun parseWarnActions(guildId: Long, params: Map<String, String>): List<WarnAction> {
        val warnActionsList = arrayListOf<WarnAction>()
        val isGuildPatron = fetchGuildPatronStatus(guildId.toString())
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
