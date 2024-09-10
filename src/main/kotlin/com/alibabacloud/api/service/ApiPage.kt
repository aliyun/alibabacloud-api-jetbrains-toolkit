package com.alibabacloud.api.service

import com.alibabacloud.api.service.apidebug.ApiDebug
import com.alibabacloud.api.service.constants.ApiConstants
import com.alibabacloud.api.service.constants.NotificationGroups
import com.alibabacloud.api.service.notification.NormalNotification
import com.alibabacloud.api.service.sdksample.SdkSample
import com.alibabacloud.api.service.util.CacheUtil
import com.alibabacloud.api.service.util.FormatUtil
import com.alibabacloud.api.service.util.RequestUtil
import com.alibabacloud.api.service.util.ResourceUtil
import com.alibabacloud.constants.PropertiesConstants
import com.alibabacloud.i18n.I18nUtils
import com.alibabacloud.telemetry.ExperienceQuestionnaire
import com.google.gson.*
import com.intellij.ide.BrowserUtil
import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentManager
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.ui.jcef.JBCefBrowserBase
import com.intellij.ui.jcef.JBCefClient
import com.intellij.ui.jcef.JBCefJSQuery
import okhttp3.OkHttpClient
import okhttp3.Request
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefLoadHandler
import org.cef.handler.CefLoadHandlerAdapter
import org.cef.network.CefRequest
import java.awt.BorderLayout
import java.io.File
import java.io.IOException
import java.net.URI
import java.time.LocalDateTime
import java.util.*
import javax.swing.JPanel
import javax.swing.JSplitPane


class ApiPage {
    companion object {
        var notificationService: NormalNotification = NormalNotification

        /**
         * show api document and debug interface
         */
        fun showApiDetail(
            apiDocContent: Content,
            contentManager: ContentManager,
            apiPanel: JPanel,
            productName: String,
            apiName: String,
            defaultVersion: String,
            project: Project,
            useCache: Boolean,
        ) {
            contentManager.setSelectedContent(apiDocContent, true)
            val browser = JBCefBrowser()
            browser.jbCefClient.setProperty(JBCefClient.Properties.JS_QUERY_POOL_SIZE, 20)

            val cacheDir = File(ApiConstants.CACHE_PATH)
            if (!cacheDir.exists()) {
                cacheDir.mkdir()
            }

            val locale = if (I18nUtils.getLocale() == Locale.CHINA) "" else "-en"
            val cacheFile = File(ApiConstants.CACHE_PATH, "$productName-$defaultVersion-$apiName$locale.html")
            val cacheMeta = File(ApiConstants.CACHE_PATH, "${productName}Meta$locale")
            val cacheEndpoints = File(ApiConstants.CACHE_PATH, "${productName}Endpoints$locale")
            var cacheContent: String? = null
            var cacheDocMeta: String? = null
            val cacheEndpointList: String?

            val sdkPanel = JPanel(BorderLayout())
            val splitPane = JSplitPane(JSplitPane.VERTICAL_SPLIT)

            if (useCache && cacheFile.exists() && cacheMeta.exists() && cacheEndpoints.exists()
                && cacheFile.length() > 0 && cacheMeta.length() > 0 && cacheEndpoints.length() > 0
                && cacheFile.lastModified() + ApiConstants.ONE_DAY.toMillis() > System.currentTimeMillis()
                && cacheMeta.lastModified() + ApiConstants.ONE_DAY.toMillis() > System.currentTimeMillis()
                && cacheEndpoints.lastModified() + ApiConstants.ONE_DAY.toMillis() > System.currentTimeMillis()
            ) {
                try {
                    cacheContent = cacheFile.readText()
                    browser.loadHTML(cacheContent)
                    cacheDocMeta = cacheMeta.readText()
                    cacheEndpointList = cacheEndpoints.readText()
                    val cacheApiDocData = Gson().fromJson(cacheDocMeta, JsonObject::class.java)
                    val endpointList = Gson().fromJson(cacheEndpointList, JsonArray::class.java)

                    execute(
                        project,
                        browser,
                        cacheApiDocData,
                        apiName,
                        defaultVersion,
                        productName,
                        endpointList,
                        sdkPanel,
                        splitPane
                    )
                    executeQuestionnaire(project, browser)
                } catch (_: IOException) {
                }

                apiPanel.removeAll()
                splitPane.apply {
                    isContinuousLayout = true
                    dividerSize = 5
                    topComponent = browser.component
                }
                apiPanel.add(splitPane)
                apiPanel.revalidate()
                apiPanel.repaint()
            }

            if (!useCache || cacheContent == null || cacheDocMeta == null || cacheContent.isEmpty() || cacheDocMeta.isEmpty()) {
                val colorList = FormatUtil.adjustColor()
                val loadingHtml =
                    ResourceUtil.load("/html/loading.html").replace("var(--background-color)", colorList[0])
                        .replace("var(--text-color)", colorList[1])

                browser.loadHTML(loadingHtml)
                apiPanel.removeAll()
                apiPanel.add(browser.component)
                apiPanel.revalidate()
                apiPanel.repaint()

                var modifiedHtml = String()

                ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Loading API Doc", true) {
                    override fun run(indicator: ProgressIndicator) {
                        val isCN = I18nUtils.getLocale() == Locale.CHINA
                        val apiLocale = if (isCN) "ZH_CN" else "EN_US"
                        val endpointLocale = if (isCN) "zh-CN" else "en-US"
                        val apiDocsUrl =
                            "https://api.aliyun.com/meta/v1/products/$productName/versions/$defaultVersion/overview.json?language=$apiLocale"
                        val overviewUrl =
                            "https://api.aliyun.com/meta/v1/products/$productName/versions/$defaultVersion/api-docs.json?language=$apiLocale"
                        val endpointUrl =
                            "https://api.aliyun.com/meta/v1/products/$productName/endpoints.json?language=$endpointLocale"

                        val apiDocData =
                            getApiDocData(project, OkHttpClientProvider.instance, apiDocsUrl, overviewUrl, cacheMeta)
                        val endpointList = getEndpointList(project, OkHttpClientProvider.instance, endpointUrl)

                        execute(
                            project,
                            browser,
                            apiDocData,
                            apiName,
                            defaultVersion,
                            productName,
                            endpointList,
                            sdkPanel,
                            splitPane
                        )

                        executeQuestionnaire(project, browser)

                        val apiMeta = apiDocData.get(ApiConstants.DEBUG_APIS).asJsonObject.get(apiName).asJsonObject
                        val ext = JsonObject()
                        ext.add("errorCodes", apiMeta.get("errorCodes"))
                        ext.add("extraInfo", apiMeta.get("extraInfo"))
                        ext.add("methods", apiMeta.get("methods"))
                        ext.add("requestParamsDescription", apiMeta.get("requestParamsDescription"))
                        ext.add("responseParamsDescription", apiMeta.get("responseParamsDescription"))
                        ext.add("responseDemo", apiMeta.get("responseDemo"))
                        ext.add("schemes", apiMeta.get("schemes"))
                        ext.add("security", apiMeta.get("security"))
                        ext.add("summary", apiMeta.get("summary"))
                        ext.add("title", apiMeta.get("title"))

                        val externalDocs = JsonObject()
                        externalDocs.addProperty("description", "去调试")
                        externalDocs.addProperty(
                            "url",
                            "https://api.aliyun.com/api/$productName/$defaultVersion/$apiName",
                        )

                        val spec = JsonObject()
                        spec.addProperty("name", apiName)
                        spec.add("description", apiMeta.get("description"))
                        spec.add("method", apiMeta.get("method"))
                        if (!isCN && apiMeta.has("parameters")) {
                            val paramsArray = apiMeta.get("parameters").asJsonArray
                            for (param in paramsArray) {
                                val paramObj = param.asJsonObject
                                if (paramObj.has("schema")) {
                                    val schema = paramObj.get("schema").asJsonObject
                                    repairCnInfoInEn(schema)
                                }
                            }
                        }
                        spec.add("parameters", apiMeta.get("parameters"))

                        if (!isCN && apiMeta.has("responses")) {
                            val response = apiMeta.get("responses").asJsonObject
                            if (response.has("200")) {
                                val responseParams = response.get("200").asJsonObject
                                if (responseParams.has("schema")) {
                                    val schema = responseParams.get("schema").asJsonObject
                                    repairCnInfoInEn(schema)
                                }
                            }
                        }
                        spec.add("responses", apiMeta.get("responses"))
                        spec.add("summary", apiMeta.get("summary"))
                        spec.add("title", apiMeta.get("title"))
                        spec.add("ext", ext)
                        spec.add("externalDocs", externalDocs)

                        val apiParams = JsonObject()
                        apiParams.addProperty("specName", "$productName::$defaultVersion")
                        apiParams.addProperty("modName", "")
                        apiParams.addProperty("name", apiName)
                        apiParams.addProperty("pageType", "document")
                        apiParams.addProperty("schemaType", "api")
                        apiParams.addProperty("name", apiName)
                        apiParams.add("spec", spec)

                        val definitions =
                            apiDocData.get(ApiConstants.API_DOC_RESP_COMPONENTS).asJsonObject.get(ApiConstants.API_DOC_RESP_SCHEMAS).asJsonObject

                        val templateHtml = ResourceUtil.load("/html/index.html")
                        modifiedHtml = templateHtml.replace("\$APIMETA", "$apiParams")
                            .replace("\$DEFS", "$definitions")
                            .replace("\$LANGUAGE", if (isCN) "cn" else "en")

                        try {
                            CacheUtil.cleanExceedCache()
                            cacheFile.writeText(modifiedHtml)
                            cacheEndpoints.writeText(endpointList.toString())
                        } catch (e: IOException) {
                            cacheFile.delete()
                            cacheEndpoints.delete()
                            notificationService.showMessage(
                                project,
                                NotificationGroups.CACHE_NOTIFICATION_GROUP,
                                I18nUtils.getMsg("cache.write.fail"),
                                "",
                                NotificationType.ERROR
                            )
                        }
                    }

                    override fun onSuccess() {
                        browser.loadHTML(modifiedHtml)
                        apiPanel.removeAll()

                        splitPane.apply {
                            isContinuousLayout = true
                            dividerSize = 5
                            topComponent = browser.component
                        }

                        apiPanel.add(splitPane)
                        apiPanel.revalidate()
                        apiPanel.repaint()
                    }

                    override fun onThrowable(error: Throwable) {
                        if (error is IOException) {
                            cacheFile.delete()
                            cacheMeta.delete()
                            notificationService.showMessage(
                                project,
                                NotificationGroups.CACHE_NOTIFICATION_GROUP,
                                I18nUtils.getMsg("cache.write.fail"),
                                "",
                                NotificationType.ERROR
                            )
                        }
                    }
                })
            }
        }

        private fun repairCnInfoInEn(element: JsonElement) {
            if (element is JsonObject) {
                for (key in element.keySet()) {
                    val value = element.get(key)
                    if (key == "title" && value != null && value is JsonPrimitive) {
                        if (containsChinese(value.asString)) {
                            element.addProperty(key, "")
                        }
                    }
                    repairCnInfoInEn(value)
                }
            }
        }

        private fun containsChinese(text: String): Boolean {
            val chineseRegex = Regex("[\\u4e00-\\u9fa5]")
            return chineseRegex.containsMatchIn(text)
        }

        fun getApiDocData(
            project: Project,
            instance: OkHttpClient,
            apiDocsUrl: String,
            overviewUrl: String,
            cacheMeta: File
        ): JsonObject {
            val apiDocsData: JsonObject
            val overviewData: JsonObject
            val apiMetaData = JsonObject()
            val apiDocsRequest = RequestUtil.createRequest(apiDocsUrl)
            val overviewRequest = RequestUtil.createRequest(overviewUrl)
            try {
                apiDocsData = doRequest(project, instance, apiDocsRequest)
                overviewData = doRequest(project, instance, overviewRequest)
                mergeJsonObjects(apiMetaData, apiDocsData, overviewData)

                try {
                    CacheUtil.cleanExceedCache()
                    cacheMeta.writeText(Gson().toJson(apiMetaData))
                } catch (e: IOException) {
                    notificationService.showMessage(
                        project,
                        NotificationGroups.CACHE_NOTIFICATION_GROUP,
                        I18nUtils.getMsg("cache.write.fail"),
                        "",
                        NotificationType.ERROR
                    )
                }

            } catch (e: IOException) {
                cacheMeta.delete()
                notificationService.showMessage(
                    project,
                    NotificationGroups.NETWORK_NOTIFICATION_GROUP,
                    I18nUtils.getMsg("api.data.fetch.fail"),
                    I18nUtils.getMsg("network.check"),
                    NotificationType.ERROR
                )
            }
            return apiMetaData
        }

        private fun doRequest(project: Project, instance: OkHttpClient, request: Request): JsonObject {
            var data = JsonObject()
            instance.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    if (responseBody != null) {
                        val docResponse = Gson().fromJson(responseBody, JsonObject::class.java)
                        data = docResponse
                    }
                } else {
                    notificationService.showMessage(
                        project,
                        NotificationGroups.NETWORK_NOTIFICATION_GROUP,
                        I18nUtils.getMsg("api.data.fetch.fail"),
                        "${I18nUtils.getMsg("request.fail.error.code")} ${response.code}, ${I18nUtils.getMsg("request.fail.error.message")} ${response.message}",
                        NotificationType.ERROR
                    )
                }
            }
            return data
        }

        private fun mergeJsonObjects(target: JsonObject, source1: JsonObject, source2: JsonObject) {
            source1.keySet().forEach { key ->
                target.add(key, source1[key])
            }

            source2.keySet().forEach { key ->
                if (!target.has(key)) {
                    target.add(key, source2[key])
                } else {
                    val existingElement = target[key]
                    val newElement = source2[key]

                    when {
                        existingElement is JsonObject && newElement is JsonObject -> {
                            mergeJsonObjects(existingElement, existingElement, newElement)
                        }

                        existingElement is JsonArray && newElement is JsonArray -> {
                            newElement.forEach { item ->
                                if (!existingElement.contains(item)) {
                                    existingElement.add(item)
                                }
                            }
                        }
                    }
                }
            }
        }

        fun getEndpointList(project: Project, instance: OkHttpClient, endpointUrl: String): JsonArray {
            var endpointList = JsonArray()
            val request = RequestUtil.createRequest(endpointUrl)
            try {
                instance.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val responseBody = response.body?.string()
                        if (responseBody != null) {
                            val endpointResponse = Gson().fromJson(responseBody, JsonObject::class.java)
                            endpointList =
                                endpointResponse.get(ApiConstants.ENDPOINT_LIST_DATA).asJsonObject.get(
                                    ApiConstants.ENDPOINT_LIST_ENDPOINTS
                                ).asJsonArray
                        }
                    } else {
                        notificationService.showMessage(
                            project,
                            NotificationGroups.NETWORK_NOTIFICATION_GROUP,
                            I18nUtils.getMsg("endpoint.fetch.fail"),
                            "${I18nUtils.getMsg("request.fail.error.code")} ${response.code}, ${I18nUtils.getMsg("request.fail.error.message")} ${response.message}",
                            NotificationType.ERROR
                        )
                    }
                }
            } catch (e: IOException) {
                notificationService.showMessage(
                    project,
                    NotificationGroups.NETWORK_NOTIFICATION_GROUP,
                    I18nUtils.getMsg("endpoint.fetch.fail"),
                    I18nUtils.getMsg("network.check"),
                    NotificationType.ERROR
                )
            }
            return endpointList
        }

        fun execute(
            project: Project,
            browser: JBCefBrowser,
            apiDocData: JsonObject,
            apiName: String,
            defaultVersion: String,
            productName: String,
            endpointList: JsonArray,
            sdkPanel: JPanel,
            splitPane: JSplitPane
        ) {
            val bodyParams = JsonObject()
            bodyParams.addProperty(ApiConstants.SDK_MAKE_CODE_BODY_API_NAME, apiName)
            bodyParams.addProperty(ApiConstants.SDK_MAKE_CODE_BODY_API_VERSION, defaultVersion)
            bodyParams.addProperty(ApiConstants.SDK_MAKE_CODE_BODY_PRODUCT, productName)
            bodyParams.addProperty(ApiConstants.SDK_MAKE_CODE_BODY_SDK_TYPE, "dara")
            bodyParams.add(ApiConstants.SDK_MAKE_CODE_BODY_PARAMS, JsonObject())

            ApiDebug.executeDebug(browser, apiDocData, apiName, endpointList, project)
            ApiDebug.executeOpenDebugResult(browser, project)
            SdkSample.executeSdk(browser) { paramsValue, regionId ->
                var endpoint = String()
                if (endpointList.size() > 0) {
                    for (element in endpointList) {
                        if (element.asJsonObject.get(ApiConstants.ENDPOINT_REGION_ID).asString == regionId) {
                            endpoint = element.asJsonObject.get(ApiConstants.ENDPOINT_PUBLIC).asString
                        }
                    }
                }

                splitPane.setResizeWeight(0.6)
                bodyParams.add(ApiConstants.SDK_MAKE_CODE_BODY_PARAMS, Gson().toJsonTree(paramsValue) as JsonObject)
                bodyParams.addProperty(ApiConstants.SDK_MAKE_CODE_BODY_ENDPOINT, endpoint)
                val demoSdkObject = SdkSample.getDemoSdk(project, bodyParams)

                ApplicationManager.getApplication().invokeLater {
                    SdkSample.sdkSamplePanel(
                        apiName, defaultVersion, productName, project, sdkPanel, demoSdkObject
                    )
                    splitPane.bottomComponent = sdkPanel
                }
            }
            executeOpenLink(browser)
        }

        fun executeQuestionnaire(project: Project, browser: JBCefBrowser) {
            ExperienceQuestionnaire(project).executeQuestionnaire(browser) { isNotice ->
                val properties = PropertiesComponent.getInstance()
                val currentDateTime = LocalDateTime.now()
                if (isNotice == "0" || isNotice == null) {
                    // fill the questionnaire
                    properties.setValue(PropertiesConstants.QUESTIONNAIRE_EXPIRATION_KEY, 14 * 24, 14 * 24)
                    properties.setValue(PropertiesConstants.QUESTIONNAIRE_LAST_PROMPT_KEY, currentDateTime.toString())
                } else {
                    // close dialog
                    properties.setValue(PropertiesConstants.QUESTIONNAIRE_EXPIRATION_KEY, 1 * 24, 14 * 24)
                    properties.setValue(PropertiesConstants.QUESTIONNAIRE_LAST_PROMPT_KEY, currentDateTime.toString())
                }
            }
        }

        private fun executeOpenLink(browser: JBCefBrowser) {
            val query = JBCefJSQuery.create(browser as JBCefBrowserBase)
            var returnLink = ""
            query.addHandler { url: String? ->
                try {
                    if (url != null) {
                        try {
                            BrowserUtil.browse(URI(url))
                        } catch (e: Exception) {
                            returnLink = url
                        }
                    }
                    return@addHandler JBCefJSQuery.Response("ok")
                } catch (e: JsonSyntaxException) {
                    return@addHandler JBCefJSQuery.Response(null, 0, "errorMsg")
                }
            }

            browser.jbCefClient.addLoadHandler(
                object : CefLoadHandlerAdapter() {
                    override fun onLoadingStateChange(
                        cefBrowser: CefBrowser,
                        isLoading: Boolean,
                        canGoBack: Boolean,
                        canGoForward: Boolean,
                    ) {
                    }

                    override fun onLoadStart(
                        cefBrowser: CefBrowser,
                        frame: CefFrame,
                        transitionType: CefRequest.TransitionType,
                    ) {
                    }

                    override fun onLoadEnd(cefBrowser: CefBrowser, frame: CefFrame, httpStatusCode: Int) {
                        cefBrowser.executeJavaScript(
                            """
                        openLinkResult($returnLink)
                        window.openLink = function(arg) {
                        ${
                            query.inject(
                                "arg",
                                "response => console.log('读取参数成功', (response))",
                                "(error_code, error_message) => console.log('读取参数失败', error_code, error_message)",
                            )
                        }
                        };

                            """.trimIndent(),
                            browser.cefBrowser.url,
                            0,
                        )
                    }

                    override fun onLoadError(
                        cefBrowser: CefBrowser,
                        frame: CefFrame,
                        errorCode: CefLoadHandler.ErrorCode,
                        errorText: String,
                        failedUrl: String,
                    ) {
                    }
                },
                browser.cefBrowser,
            )
        }
    }
}