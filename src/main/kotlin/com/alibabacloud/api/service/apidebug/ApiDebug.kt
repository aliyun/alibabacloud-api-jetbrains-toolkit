package com.alibabacloud.api.service.apidebug

import com.alibabacloud.api.service.OpenAPIClient
import com.alibabacloud.api.service.constants.ApiConstants
import com.alibabacloud.api.service.constants.NotificationGroups
import com.alibabacloud.api.service.notification.NormalNotification
import com.alibabacloud.api.service.util.FormatUtil
import com.alibabacloud.models.credentials.ConfigureFile
import com.aliyun.teautil.models.TeaUtilException
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonSyntaxException
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.ui.jcef.JBCefBrowserBase
import com.intellij.ui.jcef.JBCefJSQuery
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefLoadHandler
import org.cef.handler.CefLoadHandlerAdapter
import org.cef.network.CefRequest

class ApiDebug {
    companion object {
        fun executeDebug(
            browser: JBCefBrowser,
            apiDocData: JsonObject,
            apiName: String,
            endpointList: JsonArray,
            project: Project,
        ) {
            val query = JBCefJSQuery.create(browser as JBCefBrowserBase)

            query.addHandler { arg: String? ->
                try {
                    val paramsValue = FormatUtil.getArg(arg).first
                    val regionId = FormatUtil.getArg(arg).second
                    var debugHtml = ""
                    val config = ConfigureFile.loadConfigureFile()
                    val profile = config?.profiles?.find { it.name == config.current }
                    val accessKeyId = profile?.access_key_id
                    val accessKeySecret = profile?.access_key_secret
                    if (accessKeyId == null || accessKeySecret == null || accessKeyId == "" || accessKeySecret == "") {
                        NormalNotification.showMessage(
                            project,
                            NotificationGroups.DEBUG_NOTIFICATION_GROUP,
                            "需要登录",
                            "如需调试请先在 Add Profile 处配置用户信息",
                            NotificationType.WARNING
                        )
                    } else {
                        debugHtml = getDebugResponse(
                            paramsValue,
                            apiDocData,
                            apiName,
                            endpointList,
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
                    NormalNotification.showMessage(
                        project,
                        NotificationGroups.DEBUG_NOTIFICATION_GROUP,
                        "参数格式错误",
                        "请检查",
                        NotificationType.ERROR
                    )
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
                        populateDropdown($endpointList);
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

        private fun getDebugResponse(
            args: Map<String, Any>?,
            apiDocData: JsonObject,
            apiName: String,
            endpointList: JsonArray,
            regionId: String?,
            accessKeyId: String,
            accessKeySecret: String,
            project: Project,
        ): String {
            if (apiDocData.size() <= 0 || endpointList.size() < 0) {
                return "网络请求失败，请重试"
            }
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
            for (element in endpointList) {
                if (element.asJsonObject.get("regionId").asString == regionId) {
                    endpoint = element.asJsonObject.get("public").asString
                }
            }

            val openApiRequest = OpenAPIClient.OpenApiRequest()
            openApiRequest.headers = mutableMapOf()
            if (args != null) {
                if (product == "ROS" && args.containsKey(ApiConstants.DEBUG_PARAMS_REGION_ID)) {
                    (openApiRequest.headers as MutableMap<String, String>)["x-acs-region-id"] =
                        args[ApiConstants.DEBUG_PARAMS_REGION_ID].toString()
                }
            }

            val queries = mutableMapOf<String, Any>()
            var body: Any
            val headers = mutableMapOf<String, String>()

            val responseSchema =
                apisObject.get("responses").asJsonObject?.get("200")?.asJsonObject?.get("schema")?.asJsonObject
            val responseType = if (responseSchema == null) {
                FormatUtil._bodyType(produces)
            } else if (responseSchema.has(ApiConstants.API_RESP_RESPONSES_SCHEMA_XML)) {
                "xml"
            } else if (responseSchema.has(ApiConstants.API_RESP_RESPONSES_SCHEMA_TYPE) && responseSchema.get(
                    ApiConstants.API_RESP_RESPONSES_SCHEMA_TYPE
                )?.asString != ApiConstants.API_RESP_RESPONSES_SCHEMA_TYPE_OBJECT
            ) {
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
                                } else {
                                    val bodyMap = mutableMapOf<String, Any>()
                                    if (style == "json" && value !is String) {
                                        bodyMap[name] = Gson().toJson(value)
                                    }
                                    if (style != null && value is List<*>) {
                                        val valuesList = mutableListOf<Any>()
                                        when (style) {
                                            ApiConstants.DEBUG_NEW_PARAMS_STYLE_SIMPLE -> {
                                                bodyMap[name] = FormatUtil.joinValueArray(valuesList, value, ",")
                                            }

                                            ApiConstants.DEBUG_NEW_PARAMS_STYLE_SPACE -> {
                                                bodyMap[name] = FormatUtil.joinValueArray(valuesList, value, " ")
                                            }

                                            ApiConstants.DEBUG_NEW_PARAMS_STYLE_PIPE -> {
                                                bodyMap[name] = FormatUtil.joinValueArray(valuesList, value, "|")
                                            }

                                            ApiConstants.DEBUG_NEW_PARAMS_STYLE_REPEAT_LIST -> {
                                                bodyMap[name] = value
                                            }

                                            ApiConstants.DEBUG_NEW_PARAMS_STYLE_FLAT -> {
                                                bodyMap[name] = value
                                            }
                                        }
                                    } else {
                                        bodyMap[name] = value
                                    }
                                    body = bodyMap
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
                val message = if (teaUnretryableException.message == "Invalid URL host: \"\"") {
                    "请选择正确的服务地址"
                } else {
                    teaUnretryableException.message ?: ""
                }
                NormalNotification.showMessage(
                    project, NotificationGroups.DEBUG_NOTIFICATION_GROUP, "发生错误", message, NotificationType.ERROR
                )
            } catch (teaUtilException: TeaUtilException) {
                NormalNotification.showMessage(
                    project,
                    NotificationGroups.DEBUG_NOTIFICATION_GROUP,
                    "发生错误",
                    "参数格式错误，欢迎反馈",
                    NotificationType.ERROR
                )
            }
            response["cost"] = duration
            return Gson().toJson(response)
        }
    }
}