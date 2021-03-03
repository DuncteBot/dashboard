package com.dunctebot.dashboard

import com.dunctebot.dashboard.tasks.DashboardTasks
import org.slf4j.LoggerFactory

fun main() {
    val logger = LoggerFactory.getLogger("Main")

    // start tasks
    DashboardTasks()

    server.start()

    logger.info("Dashboard ready")
}
