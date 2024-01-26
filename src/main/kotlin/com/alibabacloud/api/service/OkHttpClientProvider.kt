package com.alibabacloud.api.service

import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.Protocol
import java.util.*
import java.util.concurrent.TimeUnit

object OkHttpClientProvider {
    private val dispatcher = Dispatcher().apply {
        maxRequests = 20
        maxRequestsPerHost = 5
    }

    val instance: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .dispatcher(dispatcher)
            .protocols(Collections.singletonList(Protocol.HTTP_1_1))
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }
}