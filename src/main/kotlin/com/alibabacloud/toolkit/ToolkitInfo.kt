package com.alibabacloud.toolkit

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginDescriptor
import com.intellij.openapi.extensions.PluginId

object ToolkitInfo {
    const val PLUGIN_ID = "alibabacloud.developer.toolkit"

        val DESCRIPTOR: PluginDescriptor? by lazy {
        PluginManagerCore.getPlugin(PluginId.getId(PLUGIN_ID))
    }
}