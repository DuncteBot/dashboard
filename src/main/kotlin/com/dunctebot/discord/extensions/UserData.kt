package com.dunctebot.discord.extensions

import discord4j.discordjson.json.UserData

val UserData.asTag: String
    get() = "${this.username()}#${this.discriminator()}"
