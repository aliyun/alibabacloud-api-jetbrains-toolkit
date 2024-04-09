package com.alibabacloud.api.service.util

import okhttp3.Request
import okhttp3.RequestBody

object RequestUtil {
    fun createRequest(url: String, method: String = "GET", body: RequestBody? = null): Request {
        val builder = Request.Builder().url(url)
        when (method.uppercase()) {
            "GET" -> {}
            "POST" -> {
                if (body != null) {
                    builder.post(body)
                }
            }
        }
        return builder.build()
    }
}