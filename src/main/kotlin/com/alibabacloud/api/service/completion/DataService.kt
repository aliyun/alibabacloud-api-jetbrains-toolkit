package com.alibabacloud.api.service.completion

import com.alibabacloud.api.service.OkHttpClientProvider
import com.alibabacloud.api.service.constants.ApiConstants
import com.alibabacloud.api.service.constants.NotificationGroups
import com.alibabacloud.api.service.notification.NormalNotification
import com.alibabacloud.api.service.util.CacheUtil
import com.alibabacloud.api.service.util.RequestUtil
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.intellij.notification.NotificationType
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import java.io.File
import java.io.IOException

val cacheNameAndVersionFile = File(ApiConstants.CACHE_PATH, "nameAndVersion1")

object DataService {
    @Volatile
    private var _javaIndex: MutableMap<String, String>? = null

    @Volatile
    private var isLoaded = false

    private var isLoading = false

    fun loadMeta(project: Project): Map<String, String> {
        isLoading = true
        if (_javaIndex == null) {
            synchronized(this) {
                if (_javaIndex == null) {
                    _javaIndex = mutableMapOf()
                    ProgressManager.getInstance().run(object : Task.Backgroundable(project, "拉取元数据...", true) {
                        override fun run(indicator: ProgressIndicator) {
                            try {
                                val productAndVersion = getProduct(project)
                                getJavaIndex(project, productAndVersion)
                            } catch (e: IOException) {
                                NormalNotification.showMessage(
                                    project,
                                    NotificationGroups.NETWORK_NOTIFICATION_GROUP,
                                    "拉取元数据失败",
                                    "请检查网络",
                                    NotificationType.ERROR
                                )
                            }
                        }

                        override fun onSuccess() {
                            super.onSuccess()
                            synchronized(this@DataService) {
                                isLoaded = true
                                isLoading = false
                            }
                        }
                    })
                }
            }
        }
        return _javaIndex!!
    }

    fun refreshMeta(project: Project) {
        if (!isLoading) {
            _javaIndex = null
            isLoaded = false
            UnLoadNotificationState.hasShown = false
            loadMeta(project)
        } else {
            NormalNotification.showMessage(
                project,
                NotificationGroups.NETWORK_NOTIFICATION_GROUP,
                "正在拉取元数据",
                "请稍后重试",
                NotificationType.WARNING
            )
        }
    }

    fun getProduct(project: Project): MutableMap<String, String> {
        val productUrl = "https://api.aliyun.com/meta/v1/products.json"
        val request = RequestUtil.createRequest(productUrl)
        val productAndVersion = mutableMapOf<String, String>()
        if (cacheNameAndVersionFile.exists() && cacheNameAndVersionFile.lastModified() + ApiConstants.ONE_DAY.toMillis() > System.currentTimeMillis()) {
            val cacheNameAndVersionMap = CacheUtil.readMapCache(cacheNameAndVersionFile)
            for ((_, list) in cacheNameAndVersionMap) {
                productAndVersion[list[0]] = list[2]
            }
        } else {
            try {
                OkHttpClientProvider.instance.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val responseBody = response.body?.string()
                        if (responseBody != null) {
                            val docResponse = Gson().fromJson(responseBody, JsonArray::class.java)
                            for (doc in docResponse) {
                                val docObject = doc.asJsonObject
                                val productName = docObject.get("code").asString
                                val defaultVersion = docObject.get("defaultVersion").asString
                                productAndVersion[productName] = defaultVersion
                            }
                        }
                    } else {
                        NormalNotification.showMessage(
                            project,
                            NotificationGroups.NETWORK_NOTIFICATION_GROUP,
                            "拉取产品元数据失败",
                            "网络请求失败，错误码 ${response.code}, 错误信息 ${response.message}",
                            NotificationType.ERROR
                        )
                    }
                }
            } catch (e: IOException) {
                throw e
            }

        }
        return productAndVersion
    }

    fun getJavaIndex(project: Project, productAndVersion: MutableMap<String, String>) {
        for ((productName, defaultVersion) in productAndVersion) {
            val apiUrl =
                "https://api.aliyun.com/meta/v1/products/$productName/versions/$defaultVersion/overview.json"
            val request = RequestUtil.createRequest(apiUrl)
            try {
                OkHttpClientProvider.instance.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val responseBody = response.body?.string()
                        if (responseBody != null) {
                            val jsonResponse = Gson().fromJson(
                                responseBody,
                                JsonObject::class.java
                            )
                            val apisObject = jsonResponse.get("apis")?.asJsonObject
                                ?: JsonObject()
                            for ((key, value) in apisObject.entrySet()) {
                                val title =
                                    if (value != null && value.isJsonObject) {
                                        val titleElement =
                                            value.asJsonObject.get("title")
                                        if (titleElement != null && !titleElement.isJsonNull) {
                                            titleElement.asString
                                        } else {
                                            "暂无描述"
                                        }
                                    } else {
                                        "暂无描述"
                                    }
                                _javaIndex!!["$key::$productName::$defaultVersion"] = title
                            }
                        }
                    } else {
                        NormalNotification.showMessage(
                            project,
                            NotificationGroups.NETWORK_NOTIFICATION_GROUP,
                            "拉取 API 元数据失败",
                            "网络请求失败，错误码 ${response.code}, 错误信息 ${response.message}",
                            NotificationType.ERROR
                        )
                    }
                }
            } catch (e: IOException) {
                throw e
            }
        }
    }

    val javaIndex: Map<String, String>
        get() = _javaIndex ?: mutableMapOf()

    fun isDataLoaded(): Boolean {
        return isLoaded
    }
}

object UnLoadNotificationState {
    var hasShown = false
}