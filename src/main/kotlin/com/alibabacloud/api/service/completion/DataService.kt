package com.alibabacloud.api.service.completion

import com.alibabacloud.api.service.OkHttpClientProvider
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import okhttp3.Request

object DataService {
    @Volatile
    private var _javaIndex: MutableMap<String, String>? = null

    @Volatile
    private var isLoaded = false


    fun loadMeta(project: Project): Map<String, String> {
        if (_javaIndex == null) {
            synchronized(this) {
                if (_javaIndex == null) {
                    _javaIndex = mutableMapOf()
                    val productUrl = "https://api.aliyun.com/meta/v1/products.json"
                    var request = Request.Builder().url(productUrl).build()
                    val productAndVersion = mutableMapOf<String, String>()

                    ProgressManager.getInstance().run(object : Task.Backgroundable(project, "拉取元数据...", true) {
                        override fun run(indicator: ProgressIndicator) {
                            // TODO 刷新元数据
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
                                }
                            }

                            for ((productName, defaultVersion) in productAndVersion) {
                                val apiUrl =
                                    "https://api.aliyun.com/meta/v1/products/$productName/versions/$defaultVersion/overview.json"
                                request = Request.Builder().url(apiUrl).build()
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
                                    }
                                }
                            }
                        }

                        override fun onSuccess() {
                            super.onSuccess()
                            synchronized(this@DataService) {
                                isLoaded = true
                            }
                        }
                    })
                }
            }
        }
        return _javaIndex!!
    }

    val javaIndex: Map<String, String>
        get() = _javaIndex ?: mutableMapOf()

    fun isDataLoaded(): Boolean {
        return isLoaded
    }
}