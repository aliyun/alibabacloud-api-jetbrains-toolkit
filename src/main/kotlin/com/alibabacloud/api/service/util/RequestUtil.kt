package com.alibabacloud.api.service.util

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.application.ApplicationNamesInfo
import com.intellij.openapi.extensions.PluginId
import okhttp3.Request
import okhttp3.RequestBody

object RequestUtil {
    fun createRequest(url: String, method: String = "GET", body: RequestBody? = null): Request {
        val pluginVersion =
            PluginManagerCore.getPlugin(PluginId.getId("alibabacloud.developer.toolkit"))?.version ?: "null"
        val ideName = ApplicationNamesInfo.getInstance().fullProductName
        val ideVersion = ApplicationInfo.getInstance().fullVersion
        val sysProps = System.getProperties()
        val userAgent = String.format(
            "Toolkit (%s; %s) alibabacloud-developer-toolkit/%s JetBrains/%s/%s",
            sysProps.getProperty("os.name"),
            sysProps.getProperty("os.arch"),
            pluginVersion,
            ideVersion,
            ideName
        )
        val builder = Request.Builder().url(url).addHeader("user-agent", userAgent)
        when (method.uppercase()) {
            "GET" -> {}
            "POST" -> {
                if (body != null) {
                    builder.post(body)
                }
            }
        }
        return builder.build()
    }
}