package com.alibabacloud.api.service

import com.alibabacloud.api.service.apidebug.ApiDebug
import com.alibabacloud.api.service.constants.ApiConstants
import com.alibabacloud.api.service.sdksample.SdkSample
import com.alibabacloud.api.service.util.CacheUtil
import com.alibabacloud.api.service.util.FormatUtil
import com.alibabacloud.api.service.util.ResourceUtil
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentManager
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.ui.jcef.JBCefClient
import okhttp3.Request
import java.awt.BorderLayout
import java.io.File
import java.io.IOException
import java.net.URL
import javax.swing.JPanel
import javax.swing.JSplitPane

class ApiPage {
    companion object {
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
            val cacheFile = File(ApiConstants.CACHE_PATH, "$productName-$defaultVersion-$apiName.html")
            val cacheMeta = File(ApiConstants.CACHE_PATH, "${productName}Meta")
            val cacheEndpoints = File(ApiConstants.CACHE_PATH, "${productName}Endpoints")
            var cacheContent: String? = null
            var cacheDocMeta: String? = null
            val cacheEndpointList: String?

            val sdkPanel = JPanel(BorderLayout())
            val splitPane = JSplitPane(JSplitPane.VERTICAL_SPLIT)



            if (useCache && cacheFile.exists() && cacheMeta.exists() && cacheEndpoints.exists() && cacheFile.length() > 0 && cacheMeta.length() > 0 && cacheEndpoints.length() > 0 && cacheFile.lastModified() + ApiConstants.ONE_DAY.toMillis() > System.currentTimeMillis() && cacheMeta.lastModified() + ApiConstants.ONE_DAY.toMillis() > System.currentTimeMillis() && cacheEndpoints.lastModified() + ApiConstants.ONE_DAY.toMillis() > System.currentTimeMillis()) {
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
                } catch (_: IOException) {
                }

                apiPanel.removeAll()
                splitPane.apply {
                    isContinuousLayout = true
                    dividerSize = 10
                    topComponent = browser.component
                }
                apiPanel.add(splitPane)
                apiPanel.revalidate()
                apiPanel.repaint()
            }

            if (cacheContent == null || cacheDocMeta == null || cacheContent.isEmpty() || cacheDocMeta.isEmpty()) {
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
                        var apiDocResponse = String()
                        val apiDocUrl =
                            URL("${ApiConstants.API_PARAM_URL}/products/$productName/versions/$defaultVersion/api-docs.json")
                        var apiDocData = JsonObject()
                        var endpointList = JsonArray()

                        var request = Request.Builder().url(apiDocUrl).build()
                        OkHttpClientProvider.instance.newCall(request).execute().use { response ->
                            if (response.isSuccessful) {
                                val responseBody = response.body?.string()
                                if (responseBody != null) {
                                    apiDocResponse = responseBody
                                    val docResponse = Gson().fromJson(responseBody, JsonObject::class.java)
                                    apiDocData = docResponse
                                }
                            }
                        }

                        val endpointUrl =
                            URL("https://api.aliyun.com/meta/v1/products/$productName/endpoints.json?language=zh-CN")

                        request = Request.Builder().url(endpointUrl).build()
                        OkHttpClientProvider.instance.newCall(request).execute().use { response ->
                            if (response.isSuccessful) {
                                val responseBody = response.body?.string()
                                if (responseBody != null) {
                                    val endpointResponse = Gson().fromJson(responseBody, JsonObject::class.java)
                                    endpointList =
                                        endpointResponse.get(ApiConstants.ENDPOINT_LIST_DATA).asJsonObject.get(
                                            ApiConstants.ENDPOINT_LIST_ENDPOINTS
                                        ).asJsonArray
                                }
                            }
                        }

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
                        spec.add("parameters", apiMeta.get("parameters"))
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
                        modifiedHtml = templateHtml.replace("\$APIMETA", "$apiParams").replace("\$DEFS", "$definitions")
                        try {
                            CacheUtil.cleanExceedCache()
                            cacheFile.writeText(modifiedHtml)
                            cacheMeta.writeText(apiDocResponse)
                            cacheEndpoints.writeText(endpointList.toString())
                        } catch (e: IOException) {
                            throw e
                        }
                    }

                    override fun onSuccess() {
                        browser.loadHTML(modifiedHtml)
                        apiPanel.removeAll()

                        splitPane.apply {
                            isContinuousLayout = true
                            dividerSize = 10
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
                        }
                    }
                })
            }
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
        }
    }
}