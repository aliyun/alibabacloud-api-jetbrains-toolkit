package com.alibabacloud.api.service

import com.alibabacloud.api.service.util.ResourceUtil
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.junit.WireMockRule
import com.intellij.testFramework.ApplicationRule
import okhttp3.Request
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class OkHttpClientProviderTest {
    @Rule
    @JvmField
    val application = ApplicationRule()

    @Rule
    @JvmField
    val wireMock = createSelfSignedServer()

    @Before
    fun setUp() {
        stubFor(any(urlPathEqualTo("/")).willReturn(aResponse().withStatus(200)))
    }

    @Test
    fun testCertGetsTrusted() {
        val client = OkHttpClientProvider.instance
        val request = Request.Builder()
            .url("https://localhost:${wireMock.httpsPort()}/")
            .build()

        val response = client.newCall(request).execute()
        assertThat(response.isSuccessful).isTrue()
    }

    private fun createSelfSignedServer(): WireMockRule {
        val selfSignedJks = ResourceUtil.loadPath("/testcert.jks")
        return WireMockRule(
            wireMockConfig()
                .dynamicHttpsPort()
                .keystorePath(selfSignedJks)
                .keystorePassword("certtest")
                .keyManagerPassword("certtest")
                .keystoreType("jks")
        )
    }
}