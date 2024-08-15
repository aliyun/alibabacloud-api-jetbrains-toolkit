package com.alibabacloud.models.api

data class ShortApiInfo(val apiInfo: ApiInfo, val isValidImport: Boolean)
data class ApiInfo(val apiName: String, val productName: String, val defaultVersion: String)