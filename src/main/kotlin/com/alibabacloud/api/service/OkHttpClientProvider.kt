package com.alibabacloud.api.service

import okhttp3.Dispatcher
import okhttp3.OkHttpClient

object OkHttpClientProvider {
    private val dispatcher = Dispatcher().apply {
        maxRequests = 20
        maxRequestsPerHost = 5
    }

    val instance: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .dispatcher(dispatcher)
            .build()
    }
}