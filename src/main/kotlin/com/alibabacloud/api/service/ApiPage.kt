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
import java.awt.BorderLayout
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
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
            var cacheContent: String? = null
            var cacheDocMeta: String? = null

            val sdkPanel = JPanel(BorderLayout())
            val splitPane = JSplitPane(JSplitPane.VERTICAL_SPLIT)

            if (useCache && cacheFile.exists() && cacheMeta.exists() && cacheFile.length() > 0 && cacheMeta.length() > 0 && cacheFile.lastModified() + ApiConstants.ONE_DAY.toMillis() > System.currentTimeMillis() && cacheMeta.lastModified() + ApiConstants.ONE_DAY.toMillis() > System.currentTimeMillis()) {
                try {
                    cacheContent = cacheFile.readText()
                    browser.loadHTML(cacheContent)
                    cacheDocMeta = cacheMeta.readText()
                    val cacheApiDocData = Gson().fromJson(cacheDocMeta, JsonObject::class.java)
                    val endpoints = cacheApiDocData.get(ApiConstants.API_DOC_ENDPOINTS).asJsonArray
                    execute(
                        project,
                        browser,
                        cacheApiDocData,
                        apiName,
                        defaultVersion,
                        productName,
                        endpoints,
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
                        val apiDocUrl =
                            URL("${ApiConstants.API_PARAM_URL}/products/$productName/versions/$defaultVersion/api-docs.json")
                        val apiDocConnection = apiDocUrl.openConnection() as HttpURLConnection
                        var apiDocData = JsonObject()
                        var endpoints = JsonArray()
                        var apiDocResponse = String()

                        if (apiDocConnection.responseCode == HttpURLConnection.HTTP_OK) {
                            apiDocResponse = apiDocConnection.inputStream.bufferedReader().use { it.readText() }
                            val docJsonResponse = Gson().fromJson(apiDocResponse, JsonObject::class.java)
                            apiDocData = docJsonResponse
                            endpoints = docJsonResponse.get(ApiConstants.API_DOC_ENDPOINTS).asJsonArray
                        }
                        apiDocConnection.disconnect()

                        execute(
                            project,
                            browser,
                            apiDocData,
                            apiName,
                            defaultVersion,
                            productName,
                            endpoints,
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
//                            cacheFile.writeText(modifiedHtml)
                            cacheFile.writeText("")
                            cacheMeta.writeText("")
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
            endpoints: JsonArray,
            sdkPanel: JPanel,
            splitPane: JSplitPane
        ) {
            val bodyParams = JsonObject()
            bodyParams.addProperty("apiName", apiName)
            bodyParams.addProperty("apiVersion", defaultVersion)
            bodyParams.addProperty("product", productName)
            bodyParams.addProperty("sdkType", "dara")
            bodyParams.add("params", JsonObject())

            ApiDebug.executeDebug(browser, apiDocData, apiName, endpoints, project)
            SdkSample.executeSdk(browser) { paramsValue, regionId ->
                var endpoint = String()
                for (element in endpoints) {
                    if (element.asJsonObject.get("regionId").asString == regionId) {
                        endpoint = element.asJsonObject.get("endpoint").asString
                    }
                }

                splitPane.setResizeWeight(0.6)
                bodyParams.add("params", Gson().toJsonTree(paramsValue) as JsonObject)
                bodyParams.addProperty("endpoint", endpoint)
                val demoSdkObject = SdkSample.getDemoSdk(project, bodyParams)

                ApplicationManager.getApplication().invokeLater {
                    SdkSample.sdkSamplePanel(
                        apiName, defaultVersion, productName, project, sdkPanel, demoSdkObject
                    )
                }
                ApplicationManager.getApplication().invokeLater {
                    splitPane.bottomComponent = sdkPanel
                }
            }
        }
    }
}