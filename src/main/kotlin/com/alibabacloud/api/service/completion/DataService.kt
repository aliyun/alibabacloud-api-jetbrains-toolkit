package com.alibabacloud.api.service.completion

import com.alibabacloud.api.service.OkHttpClientProvider
import com.alibabacloud.api.service.constants.ApiConstants
import com.alibabacloud.api.service.constants.NotificationGroups
import com.alibabacloud.api.service.notification.NormalNotification
import com.alibabacloud.api.service.util.CacheUtil
import com.alibabacloud.api.service.util.RequestUtil
import com.alibabacloud.i18n.I18nUtils
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import java.util.*

object DataService {
    @Volatile
    private var _javaIndex: MutableMap<String, String>? = null

    @Volatile
    private var isLoaded = false

    private var isLoading = false

    fun loadMeta(project: Project, isRefresh: Boolean): Map<String, String> {
        isLoading = true
        val completionIndex = CompletionIndexPersistentComponent.getInstance()
        val needToRefresh = completionIndex.needToRefresh()
        if (!isRefresh) {
            val state = completionIndex.state
            _javaIndex = state.completionIndex
            if (_javaIndex != null && !needToRefresh) {
                isLoaded = true
                isLoading = false
            }
        }
        if (_javaIndex == null || needToRefresh) {
            synchronized(this) {
                if (_javaIndex == null || needToRefresh) {
                    _javaIndex = mutableMapOf()
                    ProgressManager.getInstance().run(object : Task.Backgroundable(project, I18nUtils.getMsg("metadata.fetch"), true) {
                        override fun run(indicator: ProgressIndicator) {
                            try {
                                val productAndVersion = getProduct(project)
                                getJavaIndex(project, productAndVersion)
                            } catch (e: IOException) {
                                NormalNotification.showMessage(
                                    project,
                                    NotificationGroups.NETWORK_NOTIFICATION_GROUP,
                                    I18nUtils.getMsg("metadata.fetch.fail"),
                                    I18nUtils.getMsg("network.check"),
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
            loadMeta(project, true)
        } else {
            NormalNotification.showMessage(
                project,
                NotificationGroups.NETWORK_NOTIFICATION_GROUP,
                I18nUtils.getMsg("metadata.fetching"),
                "",
                NotificationType.INFORMATION
            )
        }
    }

    fun getProduct(project: Project): MutableMap<String, String> {
        val productUrl = "https://api.aliyun.com/meta/v1/products.json"
        val request = RequestUtil.createRequest(productUrl)
        val productAndVersion = mutableMapOf<String, String>()
        val nameAndVersionFile = if (I18nUtils.getLocale() == Locale.CHINA) {
            File(ApiConstants.CACHE_PATH, "nameAndVersion1")
        } else {
            File(ApiConstants.CACHE_PATH, "nameAndVersion1-en")
        }
        if (nameAndVersionFile.exists() && nameAndVersionFile.lastModified() + ApiConstants.ONE_DAY.toMillis() > System.currentTimeMillis()) {
            val cacheNameAndVersionMap = CacheUtil.readMapCache(nameAndVersionFile)
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
                            I18nUtils.getMsg("product.list.fetch.fail"),
                            "${I18nUtils.getMsg("request.fail.error.code")} ${response.code}, ${I18nUtils.getMsg("request.fail.error.message")} ${response.message}",
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
                                            I18nUtils.getMsg("description.not.exist")
                                        }
                                    } else {
                                        I18nUtils.getMsg("description.not.exist")
                                    }
                                _javaIndex!!["$key::$productName::$defaultVersion"] = title
                            }
                        }
                    } else {
                        NormalNotification.showMessage(
                            project,
                            NotificationGroups.NETWORK_NOTIFICATION_GROUP,
                            I18nUtils.getMsg("api.data.fetch.fail"),
                            "${I18nUtils.getMsg("request.fail.error.code")} ${response.code}, ${I18nUtils.getMsg("request.fail.error.message")} ${response.message}",
                            NotificationType.ERROR
                        )
                    }
                }
            } catch (e: IOException) {
                throw e
            }
        }
        val completionIndex = CompletionIndexPersistentComponent.getInstance()
        val state = completionIndex.state
        state.completionIndex = _javaIndex
        completionIndex.loadState(state)
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


data class CompletionIndexState(
    var completionIndex: MutableMap<String, String>? = null
)

@State(
    name = "completionIndex",
    storages = [Storage("alibabacloud-developer-toolkit-cache.xml")]
)
class CompletionIndexPersistentComponent : PersistentStateComponent<CompletionIndexState> {
    private var state = CompletionIndexState()

    override fun getState(): CompletionIndexState {
        return state
    }

    override fun loadState(state: CompletionIndexState) {
        this.state = state
    }

    fun needToRefresh(): Boolean {
        val storageFile = PathManager.getOptionsFile("alibabacloud-developer-toolkit-cache")
        val lastModifiedTime = if (storageFile.exists()) {
            val attributes = Files.readAttributes(storageFile.toPath(), BasicFileAttributes::class.java)
            attributes.lastModifiedTime().toMillis()
        } else 0
        val time = System.currentTimeMillis() - lastModifiedTime
        return time > ApiConstants.ONE_DAY.toMillis()
    }

    companion object {
        fun getInstance(): CompletionIndexPersistentComponent {
            return service()
        }
    }
}
