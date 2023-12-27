package com.alibabacloud.api.service

import com.alibabacloud.api.service.constants.ApiConstants
import com.alibabacloud.api.service.util.FormatUtil
import com.alibabacloud.api.service.util.ResourceUtil
import com.alibabacloud.models.credentials.ConfigureFile
import com.aliyun.teautil.MapTypeAdapter
import com.aliyun.teautil.models.TeaUtilException
import com.google.common.reflect.TypeToken
import com.google.gson.*
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
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
import com.intellij.util.castSafelyTo
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefLoadHandler
import org.cef.handler.CefLoadHandlerAdapter
import org.cef.network.CefRequest
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import javax.swing.JPanel

class ProcessMeta {
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
        ) {
            contentManager.setSelectedContent(apiDocContent, true)
            val browser = JBCefBrowser()
            browser.jbCefClient.setProperty(JBCefClient.Properties.JS_QUERY_POOL_SIZE, 20)

            try {
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
                        var apiDocResponse: String

                        if (apiDocConnection.responseCode == HttpURLConnection.HTTP_OK) {
                            apiDocResponse = apiDocConnection.inputStream.bufferedReader().use { it.readText() }
                            val docJsonResponse = Gson().fromJson(apiDocResponse, JsonObject::class.java)
                            apiDocData = docJsonResponse
                            endpoints = docJsonResponse.get(ApiConstants.API_DOC_ENDPOINTS).asJsonArray
                        }

                        apiDocConnection.disconnect()

                        executeDebug(browser, apiDocData, apiName, endpoints, project)

                        val apiMeta =
                            apiDocData.get(ApiConstants.DEBUG_APIS).asJsonObject.get(apiName).asJsonObject

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
                    }

                    override fun onSuccess() {
                        browser.loadHTML(modifiedHtml)
                        apiPanel.removeAll()
                        apiPanel.add(browser.component)
                        apiPanel.revalidate()
                        apiPanel.repaint()
                    }
                })
            } catch (_: IOException) {
            }

        }

        /**
         * execute api debug
         */
        private fun executeDebug(
            browser: JBCefBrowser,
            apiDocData: JsonObject,
            apiName: String,
            endpoints: JsonArray,
            project: Project,
        ) {
            val query = JBCefJSQuery.create(browser as JBCefBrowserBase)

            query.addHandler { arg: String? ->
                try {
                    val argsType = object : TypeToken<Map<String, Any>>() {}.type
                    val gson = GsonBuilder()
                        .registerTypeAdapter(object : TypeToken<Map<String?, Any?>?>() {}.type, MapTypeAdapter())
                        .create()
                    val debugParams: Map<String, Any> = gson.fromJson(arg, argsType)
                    val paramsValue = debugParams["paramsValue"].castSafelyTo<Map<String, Any>>()
                    val regionId = debugParams["regionId"].toString()
                    var debugHtml = ""
                    val config = ConfigureFile.loadConfigureFile()
                    val profile = config.profiles.find { it.name == config.current }
                    val accessKeyId = profile?.access_key_id
                    val accessKeySecret = profile?.access_key_secret
                    if (accessKeyId == null || accessKeySecret == null || accessKeyId == "" || accessKeySecret == "") {
                        NotificationGroupManager.getInstance()
                            .getNotificationGroup("AlibabaCloud API: Warning")
                            .createNotification(
                                "需要登录：如需调试请先在 Edit Profile 处配置用户信息",
                                NotificationType.WARNING,
                            )
                            .notify(project)
                    } else {
                        debugHtml =
                            getDebugResponse(
                                paramsValue,
                                apiDocData,
                                apiName,
                                endpoints,
                                regionId,
                                accessKeyId,
                                accessKeySecret,
                                project
                            ).replace(
                                "\\\"",
                                "",
                            )
                    }

                    browser.cefBrowser.executeJavaScript(
                        "transmitDebugResponse('$debugHtml')",
                        browser.cefBrowser.url,
                        0,
                    )
                    return@addHandler JBCefJSQuery.Response("ok")
                } catch (e: JsonSyntaxException) {
                    val message = "Params format error, please check."
                    NotificationGroupManager.getInstance()
                        .getNotificationGroup("AlibabaCloud API: Warning")
                        .createNotification(message, NotificationType.WARNING)
                        .notify(project)
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
                        val regionIds = JsonArray()
                        for (i in 0 until endpoints.size()) {
                            val regionIdToSelect = endpoints[i].asJsonObject.get("regionId").asString
                            regionIds.add(regionIdToSelect)
                        }

                        cefBrowser.executeJavaScript(
                            """
                        populateDropdown($regionIds);
                        window.callLogicLayer = function(arg, cb) {
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

        fun getApiListRequest(url: URL): JsonArray {
            try {
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = ApiConstants.METHOD_GET

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val jsonResponse = Gson().fromJson(response, JsonObject::class.java)
                    val data = jsonResponse.getAsJsonArray(ApiConstants.API_DIR_DATA)
                    connection.disconnect()
                    return data
                }
                return JsonArray()
            } catch (_: IOException) {
                return JsonArray()
            }
        }

        private fun getDebugResponse(
            args: Map<String, Any>?,
            apiDocData: JsonObject,
            apiName: String,
            endpoints: JsonArray,
            regionId: String?,
            accessKeyId: String,
            accessKeySecret: String,
            project: Project,
        ): String {
            val apisObject = apiDocData.get(ApiConstants.DEBUG_APIS).asJsonObject.get(apiName).asJsonObject
            val methods = apisObject.get(ApiConstants.DEBUG_METHODS).asJsonArray
            val method = FormatUtil.getMethod(methods)
            val schemes = apisObject.get(ApiConstants.DEBUG_SCHEMES).asJsonArray
            val protocol = FormatUtil.getProtocol(schemes)
            var pathName = apisObject.get(ApiConstants.DEBUG_PATH)?.asString ?: "/"
            val produces = apisObject.get(ApiConstants.DEBUG_PRODUCES)?.asJsonArray
            val apiParameters = apisObject.get(ApiConstants.DEBUG_PARAMETERS).asJsonArray
            val newParams = mutableMapOf<String, JsonObject>()
            val info = apiDocData.get("info").asJsonObject
            val product = info.get("product").asString
            val apiVersion = info.get("version").asString

            var requestType = "json"
            apiParameters.forEach { apiParam ->
                val paramName = apiParam.asJsonObject.get(ApiConstants.DEBUG_PARAMETERS_NAME).asString
                newParams[paramName] = apiParam.asJsonObject
                if (apiParam.asJsonObject.get("in").asString == "formData") {
                    requestType = "formData"
                }
            }

            var endpoint = String()
            for (element in endpoints) {
                if (element.asJsonObject.get("regionId").asString == regionId) {
                    endpoint = element.asJsonObject.get("endpoint").asString
                }
            }

            val openApiRequest = OpenAPIClient.OpenApiRequest()
            openApiRequest.headers = mutableMapOf<String, String>()
            if (args != null) {
                if (product == "ROS" && args.containsKey(ApiConstants.DEBUG_PARAMS_REGION_ID)) {
                    (openApiRequest.headers as MutableMap<String, String>)["x-acs-region-id"] =
                        args.get(ApiConstants.DEBUG_PARAMS_REGION_ID).toString()
                }
            }

            val queries = mutableMapOf<String, Any>()
            var body = Any()
            val headers = mutableMapOf<String, String>()

            val responseSchema =
                apisObject.get("responses").asJsonObject?.get("200")?.asJsonObject?.get("schema")?.asJsonObject
            val responseType = if (responseSchema == null) {
                FormatUtil._bodyType(produces)
            } else if (responseSchema.has(ApiConstants.API_RESP_RESPONSES_SCHEMA_XML)) {
                "xml"
            } else if (responseSchema.get(ApiConstants.API_RESP_RESPONSES_SCHEMA_TYPE)?.asString != ApiConstants.API_RESP_RESPONSES_SCHEMA_TYPE_OBJECT) {
                if (responseSchema.has(ApiConstants.API_RESP_RESPONSES_SCHEMA_FORMAT)) {
                    responseSchema.get(
                        ApiConstants.API_RESP_RESPONSES_SCHEMA_FORMAT,
                    ).asString
                } else {
                    responseSchema.get(ApiConstants.API_RESP_RESPONSES_SCHEMA_TYPE).asString
                }
            } else {
                "json"
            }

            if (args != null) {
                for ((name, value) in args.entries) {
                    val paramInfo = newParams[name]?.asJsonObject
                    if (paramInfo != null) {
                        val position = paramInfo.get(ApiConstants.DEBUG_NEW_PARAMS_IN).asString
                        val style = paramInfo.get(ApiConstants.DEBUG_NEW_PARAMS_STYLE)?.asString
                        when (position) {
                            ApiConstants.DEBUG_NEW_PARAMS_POSITION_PATH -> {
                                var newValue = String()
                                if (style == "json" && value !is String) {
                                    newValue = Gson().toJson(value)
                                }
                                if (pathName.indexOf('*') != -1 && name == "requestPath") {
                                    pathName = newValue
                                } else if (pathName.contains(name)) {
                                    pathName = pathName.replace("{$name}", value.toString())
                                }
                            }

                            ApiConstants.DEBUG_NEW_PARAMS_POSITION_HOST -> {
                            }

                            ApiConstants.DEBUG_NEW_PARAMS_POSITION_QUERY -> {
                                if (style == "json" && value !is String) {
                                    val jsonString = Gson().toJson(value)
                                    queries[name] = jsonString
                                }
                                if (style != null && value is List<*>) {
                                    val valuesList = mutableListOf<Any>()
                                    when (style) {
                                        ApiConstants.DEBUG_NEW_PARAMS_STYLE_SIMPLE -> {
                                            queries[name] = FormatUtil.joinValueArray(valuesList, value, ",")
                                        }

                                        ApiConstants.DEBUG_NEW_PARAMS_STYLE_SPACE -> {
                                            queries[name] = FormatUtil.joinValueArray(valuesList, value, " ")
                                        }

                                        ApiConstants.DEBUG_NEW_PARAMS_STYLE_PIPE -> {
                                            queries[name] = FormatUtil.joinValueArray(valuesList, value, "|")
                                        }

                                        ApiConstants.DEBUG_NEW_PARAMS_STYLE_REPEAT_LIST -> {
                                            queries[name] = value
                                        }

                                        ApiConstants.DEBUG_NEW_PARAMS_STYLE_FLAT -> {
                                            queries[name] = value
                                        }
                                    }
                                } else {
                                    queries[name] = value
                                }
                            }

                            ApiConstants.DEBUG_NEW_PARAMS_POSITION_BODY,
                            ApiConstants.DEBUG_NEW_PARAMS_POSITION_FORM_DATA,
                            -> {
                                val type = paramInfo.get("schema")?.asJsonObject?.get("type")?.asString
                                if (name == "RequestBody" && type == "RequestBody") {
                                    body = value
                                }

                                if (style == "json" && value !is String) {
                                    body = Gson().toJson(value)
                                }
                                if (style != null && value is List<*>) {
                                    val valuesList = mutableListOf<Any>()
                                    when (style) {
                                        ApiConstants.DEBUG_NEW_PARAMS_STYLE_SIMPLE -> {
                                            body = FormatUtil.joinValueArray(valuesList, value, ",")
                                        }

                                        ApiConstants.DEBUG_NEW_PARAMS_STYLE_SPACE -> {
                                            body = FormatUtil.joinValueArray(valuesList, value, " ")
                                        }

                                        ApiConstants.DEBUG_NEW_PARAMS_STYLE_PIPE -> {
                                            body = FormatUtil.joinValueArray(valuesList, value, "|")
                                        }

                                        ApiConstants.DEBUG_NEW_PARAMS_STYLE_REPEAT_LIST -> {
                                            body = value
                                        }

                                        ApiConstants.DEBUG_NEW_PARAMS_STYLE_FLAT -> {
                                            body = value
                                        }
                                    }
                                } else {
                                    body = value
                                }
                                openApiRequest.body = body
                            }

                            ApiConstants.DEBUG_NEW_PARAMS_POSITION_HEADER -> {
                                if (style == "json" && value !is String) {
                                    headers[name] = Gson().toJson(value)
                                }
                                if (style != null && value is List<*>) {
                                    val valuesList = mutableListOf<Any>()
                                    when (style) {
                                        ApiConstants.DEBUG_NEW_PARAMS_STYLE_SIMPLE -> {
                                            headers[name] = FormatUtil.joinValueArray(valuesList, value, ",")
                                        }

                                        ApiConstants.DEBUG_NEW_PARAMS_STYLE_SPACE -> {
                                            headers[name] = FormatUtil.joinValueArray(valuesList, value, " ")
                                        }

                                        ApiConstants.DEBUG_NEW_PARAMS_STYLE_PIPE -> {
                                            headers[name] = FormatUtil.joinValueArray(valuesList, value, "|")
                                        }

                                        ApiConstants.DEBUG_NEW_PARAMS_STYLE_REPEAT_LIST -> {
                                            headers[name] = value.toString()
                                        }

                                        ApiConstants.DEBUG_NEW_PARAMS_STYLE_FLAT -> {
                                            headers[name] = value.toString()
                                        }
                                    }
                                } else {
                                    headers[name] = value.toString()
                                }
                                openApiRequest.headers = headers
                            }
                        }
                    }
                }
            }

            openApiRequest.query = com.aliyun.openapiutil.Client.query(queries)
            val runtimeOptions = com.aliyun.teautil.models.RuntimeOptions()
            val teaConfig = OpenAPIClient.Config()
            teaConfig.endpoint = endpoint
            teaConfig.accessKeyId = accessKeyId
            teaConfig.accessKeySecret = accessKeySecret
            teaConfig.protocol = protocol
            // TODO Credentials
//            teaConfig.credential = _credential
            teaConfig.readTimeout = 50000
            teaConfig.connectTimeout = 50000
            teaConfig.globalParameters = OpenAPIClient.GlobalParameters().setHeaders(openApiRequest.headers)

            val data = OpenAPIClient.Params()
            data.version = apiVersion
            data.method = method
            data.pathname = pathName
            data.action = apiName
            data.reqBodyType = requestType
            data.bodyType = responseType
            data.authType = "AK"
            data.protocol = protocol

            val client = OpenAPIClient(teaConfig)
            var response = mutableMapOf<String, Any?>()
            var duration = 0

            try {
                val startTime = System.currentTimeMillis()
                response = client.doRequest(data, openApiRequest, runtimeOptions).toMutableMap()
                duration = (System.currentTimeMillis() - startTime).toInt()
            } catch (teaUnretryableException: com.aliyun.tea.TeaUnretryableException) {
                val message =
                    if (teaUnretryableException.message == "Invalid URL host: \"\"") {
                        "请选择正确的服务地址"
                    } else {
                        teaUnretryableException.message
                            ?: ""
                    }
                NotificationGroupManager.getInstance()
                    .getNotificationGroup("AlibabaCloud API: Warning")
                    .createNotification(message, NotificationType.WARNING)
                    .notify(project)
            } catch (teaUtilException: TeaUtilException) {
                val message = "Some format exception, welcome to feedback"
                NotificationGroupManager.getInstance()
                    .getNotificationGroup("AlibabaCloud API: Error")
                    .createNotification(message, NotificationType.ERROR)
                    .notify(project)
            }
            response["cost"] = duration
            return Gson().toJson(response)
        }
    }
}
