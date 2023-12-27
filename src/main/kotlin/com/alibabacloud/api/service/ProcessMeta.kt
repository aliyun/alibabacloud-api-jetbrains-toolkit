package com.alibabacloud.api.service

import com.alibabacloud.api.service.constants.ApiConstants
import com.alibabacloud.api.service.util.FormatUtil
import com.alibabacloud.api.service.util.ResourceUtil
import com.alibabacloud.models.api.ErrorCodes
import com.alibabacloud.models.api.ParamList
import com.alibabacloud.models.api.Parameter
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentManager
import com.intellij.ui.jcef.JBCefBrowser
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import javax.swing.JPanel

class ProcessMeta {
    companion object {
        fun showApiDetail(
            apiDocContent: Content,
            contentManager: ContentManager,
            apiPanel: JPanel,
            productName: String,
            apiName: String,
            defaultVersion: String,
            project: Project
        ) {
            contentManager.setSelectedContent(apiDocContent, true)
            val browser = JBCefBrowser()

            var colorList = FormatUtil.adjustColor()
            val loadingHtml =
                ResourceUtil.load("/html/loading.html").replace("var(--background-color)", colorList[0])
                    .replace("var(--text-color)", colorList[1])

            browser.loadHTML(loadingHtml)

            apiPanel.removeAll()
            apiPanel.add(browser.component)
            apiPanel.revalidate()
            apiPanel.repaint()
            var modifiedHtml = String()
            try {
                ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Loading API Doc", true) {
                    override fun run(indicator: ProgressIndicator) {
                        val apiDocUrl =
                            URL("${ApiConstants.API_PARAM_URL}/products/$productName/versions/$defaultVersion/api-docs")


                        val apiDocConnection = apiDocUrl.openConnection() as HttpURLConnection
                        var refSchema = JsonObject()

                        if (apiDocConnection.responseCode == HttpURLConnection.HTTP_OK) {
                            val apiDocResponse = apiDocConnection.inputStream.bufferedReader().use { it.readText() }
                            val docJsonResponse = Gson().fromJson(apiDocResponse, JsonObject::class.java)
                            refSchema =
                                docJsonResponse.get(ApiConstants.API_DOC_RESP_DATA).asJsonObject.get(ApiConstants.API_DOC_RESP_COMPONENTS).asJsonObject.get(
                                    ApiConstants.API_DOC_RESP_SCHEMAS,
                                ).asJsonObject
                        }
                        apiDocConnection.disconnect()

                        val apiUrl =
                            URL("${ApiConstants.API_PARAM_URL}/products/$productName/versions/$defaultVersion/apis/$apiName/api")
                        val apiConnection = apiUrl.openConnection() as HttpURLConnection
                        apiConnection.requestMethod = ApiConstants.METHOD_GET
                        val description: String
                        val documentHtml: String

                        if (apiConnection.responseCode == HttpURLConnection.HTTP_OK) {
                            val apiResponse = apiConnection.inputStream.bufferedReader().use { it.readText() }
                            apiConnection.disconnect()
                            val apiJsonResponse = Gson().fromJson(apiResponse, JsonObject::class.java)
                            val data = apiJsonResponse.get(ApiConstants.API_RESP_DATA).asJsonObject
                            description =
                                if (data.has(ApiConstants.API_RESP_DESCRIPTION)) data.get(ApiConstants.API_RESP_DESCRIPTION).asString else ApiConstants.EMPTY
                            val params = data.get(ApiConstants.API_RESP_PARAMETERS).asJsonArray
                            val responseSchema =
                                data.get(ApiConstants.API_RESP_RESPONSES).asJsonObject.get(ApiConstants.RESP_SUCCESS_CODE).asJsonObject
                            val errorCodes =
                                if (data.has(ApiConstants.API_RESP_ERROR_CODES)) data.get(ApiConstants.API_RESP_ERROR_CODES).asJsonObject else JsonObject()
                            val title =
                                if (data.has(ApiConstants.API_RESP_TITLE)) data.get(ApiConstants.API_RESP_TITLE).asString else ApiConstants.EMPTY

                            documentHtml = FormatUtil.parseMdToHtml(FormatUtil.editDescription(description))
                            // 处理code标签中的超链接问题，前端TODO
                            val editHtml = FormatUtil.regexHref(FormatUtil.editHtml(documentHtml))

                            // 展示请求参数表
                            val paramsEntries = convertParamsToTable(params, refSchema)
                            val paramsTableHtml = FormatUtil.regexHref(buildHtmlTable(paramsEntries, "req"))

                            // 展示返回参数表
                            val containerType = object : TypeToken<Map<String, Parameter.Schema>>() {}.type
                            val respTableHtml: String =
                                if (!responseSchema.has(ApiConstants.API_RESP_RESPONSES_SCHEMA)) {
                                    StringBuilder().append("<h2>${ApiConstants.PARAM_TABLE_TITLE_RESP_EMPTY}</h2>")
                                        .toString()
                                } else {
                                    val responseSchemaContainer =
                                        Gson().fromJson<Map<String, Parameter.Schema>>(responseSchema, containerType)
                                    val schema = responseSchemaContainer[ApiConstants.API_RESP_RESPONSES_SCHEMA]!!
                                    FormatUtil.regexHref(responseConvert(schema, refSchema))
                                }

                            // 展示错误码参数表
                            val tableEntries = processErrorCodes(errorCodes)
                            val errorTableHtml = buildErrorTable(tableEntries)

                            colorList = FormatUtil.adjustColor()
                            val toolWindowCssTemp = ResourceUtil.load("/css/apiTab.css")
                            val toolWindowCss = toolWindowCssTemp.replace("var(--background-color)", colorList[0])
                                .replace("var(--text-color)", colorList[1])
                                .replace("var(--button-background-color)", colorList[2])
                            val templateHtml = ResourceUtil.load("/html/apiDoc.html")

                            modifiedHtml = templateHtml.replace("\$toolWindowCss", toolWindowCss)
                                .replace("\$editHtml", editHtml)
                                .replace("\$paramsTableHtml", paramsTableHtml)
                                .replace("\$respTableHtml", respTableHtml)
                                .replace("\$errorTableHtml", errorTableHtml)
                        }
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

        private fun responseConvert(schema: Parameter.Schema, refSchema: JsonObject): String {
            // 调用处理顶层 schema 的函数，传入整个 schema 以及开始的层级（顶层为0）
            val tableEntries = processRespSchema(schema, refSchema, 0)
            return buildHtmlTable(tableEntries, "resp")
        }

        private fun processParam(
            param: Parameter,
            refSchema: JsonObject,
            level: Int = 0,
            originName: String? = null,
        ): List<ParamList> {
            val entries = mutableListOf<ParamList>()
            val name = param.name
            val schema = param.schema
            val type = schema.type.orEmpty()
            val required = schema.required ?: false
            val maximum = schema.maximum?.let { "${ApiConstants.PARAM_TABLE_SCHEMA_MAX}$it" }.orEmpty()
            val minimum =
                if (maximum == "") schema.minimum?.let { "${ApiConstants.PARAM_TABLE_SCHEMA_MIN}$it" }.orEmpty() else ""
            val maxItems = schema.maxItems?.let { "${ApiConstants.PARAM_TABLE_SCHEMA_MAX_ITEMS}$it" }.orEmpty()
            val format = schema.format.orEmpty()
            val ref = schema.ref.orEmpty()

            val commonInfo = commonProcess(type, ref, schema, refSchema)
            val refType = commonInfo.first
            val fieldDetail = commonInfo.second

            val hasChildren =
                type == ApiConstants.PARAM_TYPE_ARRAY || (type == ApiConstants.PARAM_TYPE_OBJECT && ((schema.properties != null) || (schema.additionalProperties != null)))
            val className = if (level == 0 && hasChildren) "childContent-${UUID.randomUUID()}" else originName
            val buttonId = "$className-button" // 新增按钮的ID，与类名相关联

            val toggleButton = if (level == 0 && hasChildren) {
                "<button id=\"$buttonId\" onclick=\"toggleVisibility('$className', '$buttonId')\">+</button> "
            } else {
                ""
            }

            entries.add(
                ParamList(
                    "${toggleButton}${"&nbsp".repeat(level * 4)}${if (required) "*" else ""}$name<br>${
                        "&nbsp".repeat(
                            level * 4,
                        )
                    }${if (type == ApiConstants.PARAM_TYPE_ARRAY) "[$type ${"&lt;${schema.items!!.type}&gt;"}]" else if (type != "") "[$type${if (format != "") "&lt;$format&gt;" else ""}]" else if (ref !== "") "[$refType]" else ""}",
                    "$fieldDetail $maximum $minimum $maxItems",
                    level,
                    className,
                ),
            )

            when (type) {
                ApiConstants.PARAM_TYPE_ARRAY -> {
                    schema.items?.let { items ->
                        entries.addAll(processParam(Parameter("", param.`in`, items), refSchema, level + 1, className))
                    }
                }

                ApiConstants.PARAM_TYPE_OBJECT -> {
                    if (!schema.properties.isNullOrEmpty()) {
                        schema.properties.forEach { (propName, propSchema) ->
                            entries.addAll(
                                processParam(
                                    Parameter(propName, param.`in`, propSchema),
                                    refSchema,
                                    level + 1,
                                    className,
                                ),
                            )
                        }
                    } else if (schema.additionalProperties != null) {
                        schema.additionalProperties.let { additionalProperty ->
                            entries.addAll(
                                processParam(
                                    Parameter("", param.`in`, additionalProperty),
                                    refSchema,
                                    level + 1,
                                    className,
                                ),
                            )
                        }
                    }
                }
            }
            return entries
        }

        private fun processRespSchema(
            schema: Parameter.Schema,
            refSchema: JsonObject,
            level: Int = 0,
            className: String? = null,
        ): List<ParamList> {
            val entries = mutableListOf<ParamList>()
            val type = schema.type.orEmpty()
            val format = schema.format.orEmpty()
            val ref = schema.ref.orEmpty()

            val commonInfo = commonProcess(type, ref, schema, refSchema)
            val refType = commonInfo.first
            val fieldDetail = commonInfo.second

            // 处理基本类型（包括$ref）
            if (level == 0 && type != ApiConstants.PARAM_TYPE_ARRAY && type != ApiConstants.PARAM_TYPE_OBJECT) {
                entries.add(
                    ParamList(
                        if (type != "") "[${type}${if (format != "") "&lt;$format&gt;" else ""}]" else if (ref !== "") "[$refType]" else "",
                        fieldDetail,
                        0,
                    ),
                )
            }

            when (type) {
                ApiConstants.PARAM_TYPE_ARRAY -> {
                    schema.items?.let { items ->
                        subprocessRespSchema(items, entries, refSchema, level, ApiConstants.PARAM_TYPE_ARRAY, className)
                    }
                }

                ApiConstants.PARAM_TYPE_OBJECT -> {
                    if (!schema.properties.isNullOrEmpty()) {
                        schema.properties.forEach { (propName, propSchema) ->
                            subprocessRespSchema(propSchema, entries, refSchema, level, propName, className)
                        }
                    } else if (schema.additionalProperties != null) {
                        // TODO 手动测试一下map的情况
                        subprocessRespSchema(
                            schema.additionalProperties,
                            entries,
                            refSchema,
                            level,
                            ApiConstants.PARAM_TYPE_MAP,
                            className,
                        )
                    }
                }
            }
            return entries
        }

        private fun subprocessRespSchema(
            schema: Parameter.Schema,
            entries: MutableList<ParamList>,
            refSchema: JsonObject,
            level: Int,
            arrayOrObject: String,
            parentId: String? = null,
        ) {
            val format = schema.format.orEmpty()
            val type = schema.type.orEmpty()
            val ref = schema.ref.orEmpty()

            val commonInfo = commonProcess(type, ref, schema, refSchema)
            val refType = commonInfo.first
            val fieldDetail = commonInfo.second

            val hasChildren =
                schema.type == ApiConstants.PARAM_TYPE_ARRAY || (schema.type == ApiConstants.PARAM_TYPE_OBJECT && (schema.properties != null) && (schema.additionalProperties != null))
            val className = if (level == 0 && hasChildren) "childContent-${UUID.randomUUID()}" else parentId
            val buttonId = "$className-button"

            val toggleButton = if (level == 0 && hasChildren) {
                "<button id=\"$buttonId\" onclick=\"toggleVisibility('$className', '$buttonId')\">+</button> "
            } else {
                ""
            }

            val fieldName = when (arrayOrObject) {
                ApiConstants.PARAM_TYPE_ARRAY -> {
                    "${"&nbsp;".repeat(level * 4)}${if (type != "") "[${schema.type}${if (schema.type == "array") "&lt;${schema.items!!.type}&gt;" else ""}${if (format != "") "&lt;$format&gt;" else ""}]" else if (schema.ref !== "") "[$refType]" else ""}"
                }

                ApiConstants.PARAM_TYPE_MAP -> {
                    "${"&nbsp;".repeat(level * 4)}${"map"}"
                }

                else -> {
                    "${"&nbsp;".repeat(level * 4)}$arrayOrObject <br>${"&nbsp".repeat(level * 4)}${if (type != "") "[${schema.type}${if (schema.type == "array") "&lt;${schema.items!!.type}&gt;" else ""}${if (format != "") "&lt;$format&gt;" else ""}]" else if (schema.ref !== "") "[$refType]" else ""}"
                }
            }

            entries.add(
                ParamList(
                    "${toggleButton}$fieldName",
                    fieldDetail,
                    level,
                    className,
                ),
            )

            entries.addAll(processRespSchema(schema, refSchema, level + 1, className))
        }

        private fun processErrorCodes(errorCodes: JsonObject): List<ErrorCodes> {
            val entries = mutableListOf<ErrorCodes>()

            errorCodes.entrySet().forEach { (httpStatusCode, errorListJsonElement) ->
                val errorList = errorListJsonElement.asJsonArray
                errorList.forEach { errorJsonElement ->
                    val errorCode = errorJsonElement.asJsonObject.get(ApiConstants.ERROR_CODES_ERROR_CODE).asString
                    val errorMessage =
                        errorJsonElement.asJsonObject.get(ApiConstants.ERROR_CODES_ERROR_MESSAGE).asString
                    entries.add(ErrorCodes(httpStatusCode, errorCode, errorMessage))
                }
            }

            return entries
        }

        private fun commonProcess(
            type: String,
            ref: String,
            schema: Parameter.Schema,
            refSchema: JsonObject
        ): Pair<String, String> {
            val description = FormatUtil.parseMdToHtml(FormatUtil.editProps(schema.description.orEmpty(), "table"))
            val example =
                FormatUtil.parseMdToHtml(
                    FormatUtil.editProps(
                        schema.example?.let { "${ApiConstants.PARAM_TABLE_SCHEMA_EXAMPLE}$it" }
                            .orEmpty(),
                        "table",
                    ),
                )

            val refTypeAndEnum = FormatUtil.getRefTypeAndEnum(type, ref, refSchema, schema.enum)
            val refType = refTypeAndEnum.first
            val enum = refTypeAndEnum.second
            val fieldDetail = "$description $example $enum"
            return Pair(refType, fieldDetail)
        }

        private fun convertParamsToTable(paramsJsonArray: JsonArray, refSchema: JsonObject): List<ParamList> {
            val params = Gson().fromJson(paramsJsonArray, Array<Parameter>::class.java).toList()
            val uniqueNames = mutableSetOf<String>()

            val uniqueParams = params.filter { parameter ->
                if (parameter.name in uniqueNames) {
                    false
                } else {
                    uniqueNames.add(parameter.name)
                    true
                }
            }
            return uniqueParams.flatMap { param -> processParam(param, refSchema) }
        }

        private fun buildHtmlTable(tableEntries: List<ParamList>, reqOrResp: String): String {
            val tableHtmlBuilder = StringBuilder()

            if (tableEntries.isEmpty()) {
                tableHtmlBuilder.append(if (reqOrResp == "req") "<h2>${ApiConstants.PARAM_TABLE_TITLE_REQ_EMPTY}</h2>" else "<h2>${ApiConstants.PARAM_TABLE_TITLE_RESP_EMPTY}</h2>")
            } else {
                tableHtmlBuilder.append(if (reqOrResp == "req") "<h2>${ApiConstants.PARAM_TABLE_TITLE_REQ}</h2>" else "<h2>${ApiConstants.PARAM_TABLE_TITLE_RESP}</h2>")
                tableHtmlBuilder.append("<table>\n")
                tableHtmlBuilder.append(
                    """
            |<tr>
            |    <th>${ApiConstants.PARAM_TABLE_HEADER_FILED_NAME}</th>
            |    <th>${ApiConstants.PARAM_TABLE_HEADER_FILED_DETAIL}</th>
            |</tr>
                    """.trimMargin(),
                )

                for (entry in tableEntries) {
                    val isChildContent = entry.level > 0 // 判断是否是子内容: 通过level判断
                    val displayStyle = if (isChildContent) " style=\"display: none;\"" else ""
                    val idAttribute = if (isChildContent) " class=\"${entry.className}\"" else ""
                    tableHtmlBuilder.append(
                        """
        |<tr$idAttribute$displayStyle>
        |    <td>${entry.fieldName}</td>
        |    <td>${entry.fieldDetail.replace("\n", "")}</td>
        |</tr>
                        """.trimMargin(),
                    )
                }

                // 结束HTML表格
                tableHtmlBuilder.append("</table>\n")
            }
            return tableHtmlBuilder.toString()
        }

        private fun buildErrorTable(tableEntries: List<ErrorCodes>): String {
            val tableHtmlBuilder = StringBuilder()

            if (tableEntries.isEmpty()) {
                tableHtmlBuilder.append("<h2>${ApiConstants.ERROR_TABLE_TITLE_EMPTY}</h2>")
            } else {
                tableHtmlBuilder.append("<h2>${ApiConstants.ERROR_TABLE_TITLE}</h2>")
                // 开始HTML表格
                tableHtmlBuilder.append("<table>\n")
                tableHtmlBuilder.append(
                    """
        |<tr>
        |    <th>${ApiConstants.ERROR_TABLE_HEADER_HTTP_CODE}</th>
        |    <th>${ApiConstants.ERROR_TABLE_HEADER_ERROR_CODE}</th>
        |    <th>${ApiConstants.ERROR_TABLE_HEADER_ERROR_MESSAGE}</th>
        |</tr>
                    """.trimMargin(),
                )

                for (entry in tableEntries) {
                    tableHtmlBuilder.append(
                        """
        |    <td>${entry.httpStatusCode}</td>
        |    <td>${entry.errorCode}</td>
        |    <td>${entry.errorMessage}</td>
        |</tr>
                        """.trimMargin(),
                    )
                }
                tableHtmlBuilder.append("</table>\n")
            }

            return tableHtmlBuilder.toString()
        }

        fun buildDebugHtml(debugParams: JsonObject, apiName: String): String {
            val method =
                FormatUtil.getMethod(
                    debugParams.get(ApiConstants.DEBUG_PARAMS_PARAM_OBJECT).asJsonObject.get(
                        ApiConstants.DEBUG_PARAMS_PARAM_OBJECT_METHOD,
                    ).asString,
                )
            val protocol =
                if (debugParams.get(ApiConstants.DEBUG_PARAMS_PARAM_OBJECT).asJsonObject.get(ApiConstants.DEBUG_PARAMS_PARAM_OBJECT_PROTOCOL).asString.split(
                        "|",
                    )
                        .contains(ApiConstants.PROTOCOL_HTTPS)
                ) {
                    ApiConstants.PROTOCOL_HTTPS
                } else {
                    ApiConstants.PROTOCOL_HTTP
                }
            val paramObject = debugParams.get(ApiConstants.DEBUG_PARAMS_PARAM_OBJECT).asJsonObject
            val endpoint = debugParams.get(ApiConstants.DEBUG_PARAMS_PARAM_ENDPOINT).asString
            val pathName = paramObject.get(ApiConstants.DEBUG_PARAMS_PARAM_PATH).asString
            val requestType =
                if (!debugParams.get(ApiConstants.DEBUG_PARAMS_PARAM_BODY_STYLE).isJsonNull) {
                    debugParams.get(
                        ApiConstants.DEBUG_PARAMS_PARAM_BODY_STYLE,
                    ).asString
                } else {
                    null
                }

            val regionId = debugParams.get(ApiConstants.DEBUG_PARAMS_PARAM_REGION_ID).asString
            val oldParams = Gson().fromJson(
                paramObject.get(ApiConstants.DEBUG_PARAMS_PARAM_OBJECT_PARAMS).asString,
                JsonArray::class.java,
            )
            val newParams = mutableMapOf<String, String>()

            val tableHtmlBuilder = StringBuilder()

            return tableHtmlBuilder.toString()
        }
    }
}