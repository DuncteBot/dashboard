package com.dunctebot.dashboard.rendering

import spark.ModelAndView

class DbModelAndView(data: Map<String, Any>, viewName: String) : ModelAndView(data, viewName) {
    @Suppress("UNCHECKED_CAST")
    override fun getModel(): Map<String, Any> {
        return super.getModel() as Map<String, Any>
    }
}
