package com.alibabacloud.models.api

data class ErrorCodes(
    val httpStatusCode: String,
    val errorCode: String,
    val errorMessage: String,
)
