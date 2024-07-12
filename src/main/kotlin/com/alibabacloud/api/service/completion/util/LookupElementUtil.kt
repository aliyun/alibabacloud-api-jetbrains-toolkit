package com.alibabacloud.api.service.completion.util

class LookupElementUtil {
    companion object {
        internal fun getFormat(key: String): ApiInfo {
            val keyInfo = key.split("::")
            val apiName = keyInfo.getOrElse(0) { "" }
            val productName = keyInfo.getOrElse(1) { "" }
            val defaultVersion = keyInfo.getOrElse(2) { "" }
            return ApiInfo(apiName, productName, defaultVersion)
        }
    }

    data class ApiInfo(val apiName: String, val productName: String, val defaultVersion: String)
}