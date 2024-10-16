package com.alibabacloud.models.telemetry

import com.alibabacloud.telemetry.util.NetUtil
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.application.ApplicationNamesInfo
import com.intellij.openapi.extensions.PluginId

object DefaultApplicationInfo {
    val userAgent: String by lazy {
        val sysProps = System.getProperties()
        String.format(
            "Toolkit (%s; %s) alibabacloud-developer-toolkit/%s JetBrains/%s/%s",
            sysProps.getProperty("os.name"),
            sysProps.getProperty("os.arch"),
            PluginManagerCore.getPlugin(PluginId.getId("alibabacloud.developer.toolkit"))?.version ?: "null",
            ApplicationInfo.getInstance().fullVersion,
            ApplicationNamesInfo.getInstance().fullProductName
        )
    }

    val macAddr: String by lazy {
        NetUtil.mac
    }
}