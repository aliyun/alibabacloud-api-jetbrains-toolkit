package com.alibabacloud.api.service

import com.alibabacloud.api.service.constants.NotificationGroups
import com.alibabacloud.api.service.notification.NormalNotification
import com.alibabacloud.i18n.I18nUtils
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.ResponseBody
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import java.io.File
import java.io.IOException

@ExtendWith(MockitoExtension::class)
internal class ApiPageTest {
    @Mock
    lateinit var project: Project

    @Mock
    lateinit var okHttpClient: OkHttpClient

    @Mock
    lateinit var call: Call

    @Mock
    lateinit var response: Response

    @Mock
    lateinit var responseBody: ResponseBody

    @Mock
    lateinit var normalNotification: NormalNotification

    @TempDir
    lateinit var tempDir: File

    private val sampleResponse: String = """
        {
          "code": 0,
          "data": {
            "type": "regional",
            "endpoints": [
              {
                "regionId": "xxx",
                "regionName": "xxx",
                "areaId": "xxx",
                "areaName": "xxx",
                "public": "xxx",
                "vpc": "xxx"
              },
              {
                "regionId": "yyy",
                "regionName": "yyy",
                "areaId": "yyy",
                "areaName": "yyy",
                "public": "yyy",
                "vpc": "yyy"
              }
            ]
          }
        }
    """.trimIndent()

    @BeforeEach
    fun setUp() {
        ApiPage.notificationService = normalNotification
    }

    @AfterEach
    fun tearDown() {
        ApiPage.notificationService = NormalNotification
    }

    @Test
    fun `getEndpointList should return JsonArray on successful response`() {
        `when`(okHttpClient.newCall(any())).thenReturn(call)
        `when`(call.execute()).thenReturn(response)
        `when`(response.body).thenReturn(responseBody)
        `when`(responseBody.string()).thenReturn(sampleResponse)
        `when`(response.isSuccessful).thenReturn(true)
        val endpointList = ApiPage.getEndpointList(project, okHttpClient, "https://example.com/api/endpoint")

        assert(endpointList.size() == 2)
    }

    @Test
    fun `getApiDocData should return JsonObject on successful response`() {
        `when`(okHttpClient.newCall(any())).thenReturn(call)
        `when`(call.execute()).thenReturn(response)
        `when`(response.body).thenReturn(responseBody)
        `when`(responseBody.string()).thenReturn(sampleResponse)
        `when`(response.isSuccessful).thenReturn(true)
        val cacheMeta = File(tempDir, "cacheMeta")
        val apiDocData =
            ApiPage.getApiDocData(
                project,
                okHttpClient,
                "https://example.com/api/apidoc",
                "https://example.com/overview/apidoc",
                cacheMeta
            )
        assertEquals(apiDocData, Gson().fromJson(sampleResponse, JsonObject::class.java))
    }

    @Test
    fun `getEndpointList should handle unsuccessful response`() {
        `when`(okHttpClient.newCall(any())).thenReturn(call)
        `when`(call.execute()).thenReturn(response)
        `when`(response.isSuccessful).thenReturn(false)
        `when`(response.code).thenReturn(500)
        `when`(response.message).thenReturn("Internal Server Error")
        val endpointList = ApiPage.getEndpointList(project, okHttpClient, "https://example.com/api/endpoint")

        verify(normalNotification).showMessage(
            eq(project),
            any(),
            any(),
            any(),
            any()
        )
        assertTrue(endpointList.size() == 0, "Endpoint list should be empty when unsuccessful")
    }

    @Test
    fun `getApiDocData should handle unsuccessful response`() {
        `when`(okHttpClient.newCall(any())).thenReturn(call)
        `when`(call.execute()).thenReturn(response)
        `when`(response.isSuccessful).thenReturn(false)
        `when`(response.code).thenReturn(500)
        `when`(response.message).thenReturn("Internal Server Error")
        val apiDocData =
            ApiPage.getApiDocData(
                project,
                okHttpClient,
                "https://example.com/api/apidoc",
                "https://example.com/overview/apidoc",
                mock(File::class.java)
            )

        verify(normalNotification, times(2)).showMessage(
            eq(project),
            any(),
            any(),
            any(),
            any()
        )
        assertTrue(apiDocData.size() == 0, "apiDocData should be empty when unsuccessful")
    }

    @Test
    fun `getEndpointList should handle IOException`() {
        `when`(okHttpClient.newCall(any())).thenReturn(call)
        `when`(call.execute()).thenReturn(response)
        `when`(call.execute()).thenThrow(IOException::class.java)
        val endpointList = ApiPage.getEndpointList(project, okHttpClient, "https://example.com/api/endpoint")

        verify(normalNotification).showMessage(
            eq(project),
            eq(NotificationGroups.NETWORK_NOTIFICATION_GROUP),
            eq(I18nUtils.getMsg("endpoint.fetch.fail")),
            eq(I18nUtils.getMsg("network.check")),
            eq(NotificationType.ERROR)
        )
        assertTrue(endpointList.size() == 0, "Endpoint list should be empty when IOException is thrown")
    }

    @Test
    fun `getApiDocData should handle IOException`() {
        `when`(okHttpClient.newCall(any())).thenReturn(call)
        `when`(call.execute()).thenReturn(response)
        `when`(call.execute()).thenThrow(IOException::class.java)
        val apiDocData =
            ApiPage.getApiDocData(
                project,
                okHttpClient,
                "https://example.com/api/apidoc",
                "https://example.com/overview/apidoc",
                mock(File::class.java)
            )

        verify(normalNotification).showMessage(
            eq(project),
            eq(NotificationGroups.NETWORK_NOTIFICATION_GROUP),
            eq(I18nUtils.getMsg("api.data.fetch.fail")),
            eq(I18nUtils.getMsg("network.check")),
            eq(NotificationType.ERROR)
        )
        assertTrue(apiDocData.size() == 0, "apiDocData should be empty when IOException is thrown")
    }

    @Test
    fun `getSdkInfoData should success`() {
        val path = javaClass.classLoader.getResource("mockSdkInfo.json")!!.toURI().path
        val sdkInfoStr = File(path).readText()
        val sdkInfoJson = Gson().fromJson(sdkInfoStr, JsonObject::class.java)
        val sdkInfo = ApiPage.getSdkInfoData(sdkInfoJson)
        assertEquals(11, sdkInfo.size)
        val javaSdkDetail = sdkInfo["java"]
        assertEquals("com.aliyun/ecs20140526", javaSdkDetail?.sdkName)
        assertEquals("5.3.0", javaSdkDetail?.sdkPkgVersion)
        assertEquals("maven", javaSdkDetail?.sdkPkgManagementPlatform)
        assertEquals(
            """
                <pre style='white-space: pre; color: yellow;'>
                &lt;dependency&gt;
                    &lt;groupId&gt;com.aliyun&lt;/groupId&gt;
                    &lt;artifactId&gt;ecs20140526&lt;/artifactId&gt;
                    &lt;version&gt;5.3.0&lt;/version&gt;
                &lt;/dependency&gt;
                </pre>
            """.trimIndent(),
            javaSdkDetail?.sdkInstallationCommand
        )
        val pySdkDetail = sdkInfo["python"]
        assertEquals("alibabacloud_ecs20140526", pySdkDetail?.sdkName)
        assertEquals("4.3.0", pySdkDetail?.sdkPkgVersion)
        assertEquals("pypi", pySdkDetail?.sdkPkgManagementPlatform)
        assertEquals("<span style='color: yellow;'>pip install alibabacloud_ecs20140526==4.3.0</span>", pySdkDetail?.sdkInstallationCommand)
    }
}