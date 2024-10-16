package com.alibabacloud.models.telemetry

data class TelemetryData(
    val componentId: String,
    val operationId: String,
    val language: String,
    val timestamp: String,
    val product: String? = null,
    val apiVersion: String? = null,
    val apiName: String? = null,
    val ifLogin: String? = null,
    val result: String? = null,
    val position: String? = null,
    val sdkLanguage: String? = null,
    val dialogType: String? = null,
    val title: String? = null,
    val content: String? = null,
    val dialogOption: String? = null,
    val settingsStatus: String? = null,
    val errorType: String? = null,
    val errorMessage: String? = null,
)
