package com.alibabacloud.api.service

import com.intellij.util.net.ssl.CertificateManager
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.Protocol
import java.security.SecureRandom
import java.util.*
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

object OkHttpClientProvider {
    private val dispatcher = Dispatcher().apply {
        maxRequests = 20
        maxRequestsPerHost = 5
    }

    val instance: OkHttpClient by lazy {
        val trustManager = CertificateManager.getInstance().trustManager as X509TrustManager
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, arrayOf(trustManager), SecureRandom())

        OkHttpClient.Builder()
            .dispatcher(dispatcher)
            .sslSocketFactory(sslContext.socketFactory, trustManager)
            .protocols(Collections.singletonList(Protocol.HTTP_1_1))
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }
}