package com.alibabacloud.telemetry

import com.alibabacloud.models.telemetry.DefaultApplicationInfo
import com.alibabacloud.models.telemetry.TelemetryData
import com.intellij.openapi.application.ApplicationManager
import com.intellij.testFramework.ApplicationRule
import com.intellij.testFramework.DisposableRule
import com.intellij.testFramework.fixtures.BasePlatformTestCase.assertEquals
import com.intellij.testFramework.runInEdtAndWait
import io.mockk.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.Call
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class BatchDataTelemetryTest {
    @Rule
    @JvmField
    val application = ApplicationRule()

    @Rule
    @JvmField
    val disposableRule = DisposableRule()

    private lateinit var httpClient: OkHttpClient
    private lateinit var batch: DefaultBatchDataTelemetry

    @Before
    fun setUp() {
        expireTime = 0L
        mockkObject(DefaultApplicationInfo)
        every { DefaultApplicationInfo.userAgent } returns "Test User Agent"
        every { DefaultApplicationInfo.macAddr } returns "00:00:00:00:00:00"

        httpClient = mockk()
        batch = DefaultBatchDataTelemetry(httpClient, maxQueueSize = 5, batchSize = 2)
        batch.onTelemetryEnabledChange(true)
    }

    @After
    fun teaDown() {
        batch.onTelemetryEnabledChange(false)
        batch.shutdown()
    }

    private fun requestBodyToString(requestBody: RequestBody): String {
        val buffer = okio.Buffer()
        requestBody.writeTo(buffer)
        return buffer.readUtf8()
    }

    @Test
    fun `one batch test`() {
        val mockTokenCall: Call = mockk()
        val mockTokenResponse: Response = mockk()
        val mockTokenResponseBody: ResponseBody = mockk()

        val mockReportCall: Call = mockk()
        val mockReportResponse: Response = mockk()
        val mockReportResponseBody: ResponseBody = mockk()

        every { mockTokenCall.execute() } returns mockTokenResponse
        every { mockTokenResponse.isSuccessful } returns true
        every { mockTokenResponse.body } returns mockTokenResponseBody
        every { mockTokenResponseBody.string() } returns """{"success":true, "token": "test", expireTime: "1729147952000"}"""
        every { mockTokenResponse.close() } returns Unit

        every { mockReportCall.execute() } returns mockReportResponse
        every { mockReportResponse.isSuccessful } returns true
        every { mockReportResponse.body } returns mockReportResponseBody
        every { mockReportResponseBody.string() } returns """{"success":true}"""
        every { mockReportResponse.close() } returns Unit

        every { httpClient.newCall(any()) } returnsMany listOf(mockTokenCall, mockReportCall)

        repeat(2) {
            batch.enqueue(
                TelemetryData(
                    "test",
                    "alibabacloud.test",
                    "en",
                    "1729061552026",
                    product = "Test",
                    apiVersion = "xxxx-xx-xx",
                    apiName = "TestApi"
                )
            )
        }
        assertEquals(2, batch.telemetryDataQueue.size)
        batch.consume(false)
        assertEquals(0, batch.telemetryDataQueue.size)
        verify(exactly = 2) { httpClient.newCall(any()) }

        verifyOrder {
            httpClient.newCall(match { request ->
                request.url.toString() == "https://pre-api-workbench.aliyun.com/plugin/telemetry_token" &&
                        request.method == "GET" &&
                        request.headers["user-agent"] == "Test User Agent" &&
                        request.headers["x-plugin-source-ip"] == "00:00:00:00:00:00"

            })

            httpClient.newCall(match { request ->
                request.url.toString() == "https://pre-api-workbench.aliyun.com/plugin/report_telemetry_data" &&
                        request.method == "POST" &&
                        request.headers["user-agent"] == "Test User Agent" &&
                        request.headers["x-plugin-source-ip"] == "00:00:00:00:00:00" &&
                        request.headers["x-plugin-token"] == "test" &&
                        request.body?.let { requestBodyToString(it) } == """{"metadata":[{"componentId":"test","operationId":"alibabacloud.test","language":"en","timestamp":"1729061552026","product":"Test","apiVersion":"xxxx-xx-xx","apiName":"TestApi"},{"componentId":"test","operationId":"alibabacloud.test","language":"en","timestamp":"1729061552026","product":"Test","apiVersion":"xxxx-xx-xx","apiName":"TestApi"}]}"""
            })
        }

        batch.telemetryDataQueue.clear()
    }

    @Test
    fun `multi batch test`() {
        val mockTokenCall: Call = mockk()
        val mockTokenResponse: Response = mockk()
        val mockTokenResponseBody: ResponseBody = mockk()

        val mockReportCall: Call = mockk()
        val mockReportResponse: Response = mockk()
        val mockReportResponseBody: ResponseBody = mockk()

        every { mockTokenCall.execute() } returns mockTokenResponse
        every { mockTokenResponse.isSuccessful } returns true
        every { mockTokenResponse.body } returns mockTokenResponseBody
        every { mockTokenResponseBody.string() } returns """{"success":true, "token": "test", expireTime: "1729147952000"}"""
        every { mockTokenResponse.close() } returns Unit

        every { mockReportCall.execute() } returns mockReportResponse
        every { mockReportResponse.isSuccessful } returns true
        every { mockReportResponse.body } returns mockReportResponseBody
        every { mockReportResponseBody.string() } returns """{"success":true}"""
        every { mockReportResponse.close() } returns Unit

        every { httpClient.newCall(any()) } returnsMany listOf(mockTokenCall, mockReportCall)

        repeat(3) {
            batch.enqueue(
                TelemetryData(
                    "test",
                    "alibabacloud.test",
                    "en",
                    "1729061552026",
                    product = "Test",
                    apiVersion = "xxxx-xx-xx",
                    apiName = "TestApi"
                )
            )
        }
        assertEquals(3, batch.telemetryDataQueue.size)
        batch.consume(false)
        assertEquals(0, batch.telemetryDataQueue.size)
        verify(exactly = 3) { httpClient.newCall(any()) }
        batch.telemetryDataQueue.clear()
    }

    @Test
    fun `retry test`() {
        val mockCall: Call = mockk()
        every { httpClient.newCall(any()) } returns mockCall
        every { mockCall.execute() } throws IOException("Mock error")

        batch.enqueue(
            TelemetryData(
                "test",
                "alibabacloud.test",
                "en",
                "1729061552026",
                product = "Test",
                apiVersion = "xxxx-xx-xx",
                apiName = "TestApi"
            )
        )
        assertEquals(1, batch.telemetryDataQueue.size)
        batch.consume(true)
        assertEquals(1, batch.telemetryDataQueue.size)
        verify(exactly = 1) { httpClient.newCall(any()) }
        batch.telemetryDataQueue.clear()
    }

    @Test
    fun `retry dispose`() {
        val mockTokenCall: Call = mockk()
        val mockTokenResponse: Response = mockk()
        val mockTokenResponseBody: ResponseBody = mockk()

        val mockReportCall: Call = mockk()
        val mockReportResponse: Response = mockk()
        val mockReportResponseBody: ResponseBody = mockk()

        every { mockTokenCall.execute() } returns mockTokenResponse
        every { mockTokenResponse.isSuccessful } returns true
        every { mockTokenResponse.body } returns mockTokenResponseBody
        every { mockTokenResponseBody.string() } returns """{"success":true, "token": "test", expireTime: "1729147952000"}"""
        every { mockTokenResponse.close() } returns Unit

        every { mockReportCall.execute() } returns mockReportResponse
        every { mockReportResponse.isSuccessful } returns true
        every { mockReportResponse.body } returns mockReportResponseBody
        every { mockReportResponseBody.string() } returns """{"success":true}"""
        every { mockReportResponse.close() } returns Unit

        every { httpClient.newCall(any()) } returnsMany listOf(mockTokenCall, mockReportCall)

        batch.enqueue(
            TelemetryData(
                "test",
                "alibabacloud.test",
                "en",
                "1729061552026",
                product = "Test",
                apiVersion = "xxxx-xx-xx",
                apiName = "TestApi"
            )
        )
        assertEquals(1, batch.telemetryDataQueue.size)
        batch.shutdown()
        assertEquals(0, batch.telemetryDataQueue.size)

        batch.enqueue(
            TelemetryData(
                "test",
                "alibabacloud.test",
                "en",
                "1729061552026",
                product = "Test",
                apiVersion = "xxxx-xx-xx",
                apiName = "TestApi"
            )
        )
        assertEquals(1, batch.telemetryDataQueue.size)
        batch.shutdown()
        assertEquals(1, batch.telemetryDataQueue.size)
        verify(exactly = 2) { httpClient.newCall(any()) }
        batch.telemetryDataQueue.clear()
    }

    @Test
    fun `not report when disabled from start`() {
        batch.onTelemetryEnabledChange(false)

        val mockCall: Call = mockk()
        val mockResponse: Response = mockk()
        val mockResponseBody: ResponseBody = mockk()

        every { httpClient.newCall(any()) } returns mockCall
        every { mockCall.execute() } returns mockResponse
        every { mockResponse.isSuccessful } returns true
        every { mockResponse.body } returns mockResponseBody
        every { mockResponseBody.string() } returns """{"success":true, "expireTime": "1729086752000"}"""

        batch.enqueue(
            TelemetryData(
                "test",
                "alibabacloud.test",
                "en",
                "1729061552026",
                product = "Test",
                apiVersion = "xxxx-xx-xx",
                apiName = "TestApi"
            )
        )
        assertEquals(0, batch.telemetryDataQueue.size)
        batch.consume(false)
        verify(exactly = 0) { httpClient.newCall(any()) }
        batch.onTelemetryEnabledChange(true)
    }

    @Test
    fun `not report when disabled midway`() {
        val mockCall: Call = mockk()
        val mockResponse: Response = mockk()
        val mockResponseBody: ResponseBody = mockk()

        every { httpClient.newCall(any()) } returns mockCall
        every { mockCall.execute() } returns mockResponse
        every { mockResponse.isSuccessful } returns true
        every { mockResponse.body } returns mockResponseBody
        every { mockResponseBody.string() } returns """{"success":true, "expireTime": "1729086752000"}"""

        batch.enqueue(
            TelemetryData(
                "test",
                "alibabacloud.test",
                "en",
                "1729061552026",
                product = "Test",
                apiVersion = "xxxx-xx-xx",
                apiName = "TestApi"
            )
        )
        assertEquals(1, batch.telemetryDataQueue.size)
        // will clear queue
        batch.onTelemetryEnabledChange(false)
        assertEquals(0, batch.telemetryDataQueue.size)

        batch.enqueue(
            TelemetryData(
                "test",
                "alibabacloud.test",
                "en",
                "1729061552026",
                product = "Test",
                apiVersion = "xxxx-xx-xx",
                apiName = "TestApi"
            )
        )
        assertEquals(0, batch.telemetryDataQueue.size)
        batch.consume(false)
        verify(exactly = 0) { httpClient.newCall(any()) }
    }

    @Test
    fun `bgContext should not run on UI thread`() {
        runInEdtAndWait {
            assertEquals(true, ApplicationManager.getApplication().isDispatchThread)
            runBlocking {
                withContext(batch.bgContext) {
                    assertEquals(false, ApplicationManager.getApplication().isDispatchThread)
                }
            }
        }
    }
}