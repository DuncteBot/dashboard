package com.dunctebot.dashboard.rendering

import io.javalin.http.Context
import io.javalin.plugin.rendering.FileRenderer
import org.apache.velocity.VelocityContext
import org.apache.velocity.app.VelocityEngine
import org.apache.velocity.runtime.RuntimeConstants
import spark.ModelAndView
import spark.TemplateEngine
import java.io.StringWriter
import java.util.*

class VelocityRenderer : TemplateEngine(), FileRenderer {
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

    override fun render(modelAndView: ModelAndView): String {
        if (modelAndView !is DbModelAndView) {
            throw IllegalArgumentException("modelAndView is not a of correct type")
        }

        val modelMap = modelAndView.model
        val template = velocityEngine.getTemplate("views/${modelAndView.viewName}")
        val context = VelocityContext(modelMap)
        val writer = StringWriter()

        template.merge(context, writer)

        return writer.toString()
    }

    override fun render(filePath: String, model: MutableMap<String, Any>, context: Context): String {
        val template = velocityEngine.getTemplate("views/$filePath")
        val velCtx = VelocityContext(model)
        val writer = StringWriter()

        template.merge(velCtx, writer)

        return writer.toString()
    }
}
