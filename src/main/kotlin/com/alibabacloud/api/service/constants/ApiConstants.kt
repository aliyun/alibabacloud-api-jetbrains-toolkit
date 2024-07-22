package com.alibabacloud.api.service.constants

import java.io.File
import java.time.Duration

object ApiConstants {
    var CACHE_PATH = System.getProperty("user.home") + File.separator + ".api-cache"
    val ONE_DAY: Duration = Duration.ofDays(1)
    var MAX_CACHE_NUM = 200

    val FILE_MAP: Map<String, String> = mapOf(
        "java" to "Sample.java",
        "java-async" to "Sample.java",
        "python" to "Sample.py",
        "typescript" to "client.ts",
        "go" to "main.go",
        "php" to "Sample.php"
    )

    val SUFFIX_MAP: Map<String, String> = mapOf(
        "java" to "java",
        "java-async" to "java",
        "python" to "py",
        "typescript" to "ts",
        "go" to "go",
        "php" to "php"
    )

    const val TOOLWINDOW_APIS = "Alibaba Cloud APIs"

    const val API_DIR_DATA = "data"
    const val SDK_MAKE_CODE_DATA = "data"
    const val SDK_MAKE_CODE_DEMO = "demoSdk"
    const val SDK_MAKE_CODE_BODY_API_NAME = "apiName"
    const val SDK_MAKE_CODE_BODY_API_VERSION = "apiVersion"
    const val SDK_MAKE_CODE_BODY_PRODUCT = "product"
    const val SDK_MAKE_CODE_BODY_SDK_TYPE = "sdkType"
    const val SDK_MAKE_CODE_BODY_PARAMS = "params"
    const val SDK_MAKE_CODE_BODY_ENDPOINT = "endpoint"

    const val ENDPOINT_PUBLIC = "public"
    const val ENDPOINT_REGION_ID = "regionId"
    const val ENDPOINT_LIST_DATA = "data"
    const val ENDPOINT_LIST_ENDPOINTS = "endpoints"

    const val API_RESP_RESPONSES_SCHEMA_XML = "xml"
    const val API_RESP_RESPONSES_SCHEMA_TYPE = "type"
    const val API_RESP_RESPONSES_SCHEMA_FORMAT = "format"
    const val API_RESP_RESPONSES_SCHEMA_TYPE_OBJECT = "object"

    const val API_DOC_RESP_COMPONENTS = "components"
    const val API_DOC_RESP_SCHEMAS = "schemas"

    const val API_DIR_RESPONSE_NAME = "name"
    const val API_DIR_RESPONSE_NODE_TITLE = "node_title"
    const val API_DIR_RESPONSE_TITLE = "title"
    const val API_DIR_RESPONSE_DIR_ID = "dir_id"
    const val API_DIR_RESPONSE_CHILDREN = "children"

    const val DEBUG_METHODS = "methods"
    const val DEBUG_APIS = "apis"
    const val DEBUG_SCHEMES = "schemes"
    const val DEBUG_PATH = "path"
    const val DEBUG_PRODUCES = "produces"
    const val DEBUG_PARAMETERS = "parameters"
    const val DEBUG_PARAMETERS_NAME = "name"

    const val DEBUG_PARAMS_REGION_ID = "RegionId"

    const val DEBUG_NEW_PARAMS_STYLE = "style"
    const val DEBUG_NEW_PARAMS_STYLE_SIMPLE = "simple"
    const val DEBUG_NEW_PARAMS_STYLE_SPACE = "spaceDelimited"
    const val DEBUG_NEW_PARAMS_STYLE_PIPE = "pipeDelimited"
    const val DEBUG_NEW_PARAMS_STYLE_REPEAT_LIST = "repeatList"
    const val DEBUG_NEW_PARAMS_STYLE_FLAT = "flat"

    const val DEBUG_NEW_PARAMS_IN = "in"
    const val DEBUG_NEW_PARAMS_POSITION_PATH = "path"
    const val DEBUG_NEW_PARAMS_POSITION_HOST = "host"
    const val DEBUG_NEW_PARAMS_POSITION_QUERY = "query"
    const val DEBUG_NEW_PARAMS_POSITION_BODY = "body"
    const val DEBUG_NEW_PARAMS_POSITION_HEADER = "header"
    const val DEBUG_NEW_PARAMS_POSITION_FORM_DATA = "formData"
}
