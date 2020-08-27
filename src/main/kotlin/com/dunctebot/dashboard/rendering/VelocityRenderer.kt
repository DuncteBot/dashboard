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

package com.dunctebot.dashboard.rendering

import io.github.cdimascio.dotenv.Dotenv
import org.apache.velocity.VelocityContext
import org.apache.velocity.app.VelocityEngine
import org.apache.velocity.runtime.RuntimeConstants
import spark.ModelAndView
import spark.TemplateEngine
import java.io.StringWriter
import java.util.*

class VelocityRenderer(env: Dotenv) : TemplateEngine() {
    private val velocityEngine: VelocityEngine

    init {
        val properties = Properties()

        if (env["IS_LOCAL"]!!.toBoolean()) {
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
}
