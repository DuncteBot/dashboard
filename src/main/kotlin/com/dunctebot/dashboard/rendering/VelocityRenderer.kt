package com.dunctebot.dashboard.rendering

import io.javalin.http.Context
import io.javalin.plugin.rendering.FileRenderer
import org.apache.velocity.VelocityContext
import org.apache.velocity.app.VelocityEngine
import org.apache.velocity.runtime.RuntimeConstants
import java.io.StringWriter
import java.util.*

class VelocityRenderer : FileRenderer {
    private val velocityEngine: VelocityEngine

    init {
        val properties = Properties()

        if (System.getenv("IS_LOCAL").toBoolean()) {
            // load templates from file for instant-reload when developing
            properties.setProperty(RuntimeConstants.RESOURCE_LOADERS, "file")
            properties.setProperty(
                RuntimeConstants.FILE_RESOURCE_LOADER_PATH,
                "${System.getProperty("user.dir")}/src/main/resources/"
            )
        } else {
            // load templates from jar
            properties.setProperty(RuntimeConstants.RESOURCE_LOADERS, "class")
            properties.setProperty(
                "resource.loader.class.class",
                "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader"
            )
        }

        this.velocityEngine = VelocityEngine(properties)
    }

    override fun render(filePath: String, model: MutableMap<String, Any>, context: Context): String {
        val template = velocityEngine.getTemplate("views/$filePath")
        val velCtx = VelocityContext(model)
        val writer = StringWriter()

        template.merge(velCtx, writer)

        return writer.toString()
    }
}
