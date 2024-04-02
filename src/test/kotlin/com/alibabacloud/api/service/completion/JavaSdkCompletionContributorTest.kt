package com.alibabacloud.api.service.completion

import com.alibabacloud.api.service.ApiPage
import com.alibabacloud.api.service.constants.NotificationGroups
import com.alibabacloud.api.service.notification.NormalNotification
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import java.io.IOException

@ExtendWith(MockitoExtension::class)
internal class JavaSdkCompletionContributorTest {
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

    @InjectMocks
    lateinit var javaSdkCompletionContributor: JavaSdkCompletionContributor

    private val sampleResponse: String = """
        {
            "code": 0,
            "data": {
                "demoSdk": {
                    "java": {
                        "codeSample": "Java Code Sample.",
                        "importList": [
                            "import com.aliyun.tea.*;"
                        ]
                    },
                    "java-async": {
                        "codeSample": "Java Async Code Sample.",
                        "importList": [
                            "import com.aliyun.tea.*;"
                        ]
                    }
                },
                "apiInfo": {
                    "apiStyle": "xxx",
                    "product": "xxx",
                    "apiVersion": "xxx",
                    "apiName": "xxx"
                },
                "cost": 431
                }
        }
    """.trimIndent()

    @BeforeEach
    fun setUp() {
        `when`(okHttpClient.newCall(any())).thenReturn(call)
        `when`(call.execute()).thenReturn(response)
        ApiPage.notificationService = normalNotification
    }

    @AfterEach
    fun tearDown() {
        ApiPage.notificationService = NormalNotification
    }

    @Test
    fun `getDemoSdk should return correct sample code on successful response`() {
        `when`(response.isSuccessful).thenReturn(true)
        `when`(response.body).thenReturn(responseBody)
        `when`(responseBody.string()).thenReturn(sampleResponse)

        val postRequest = Request.Builder().url("https://api.aliyun.com/api/product/makeCode")
            .post("{}".toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        val demoSdkJava = javaSdkCompletionContributor.getDemoSdk(project, okHttpClient, postRequest, "java")
        val demoSdkJavaAsync = javaSdkCompletionContributor.getDemoSdk(project, okHttpClient, postRequest, "java-async")
        assertEquals("Java Code Sample.", demoSdkJava)
        assertEquals("Java Async Code Sample.", demoSdkJavaAsync)
    }

    @Test
    fun `getDemoSdk should handle unsuccessful response`() {
        `when`(response.isSuccessful).thenReturn(false)

        val postRequest = Request.Builder().url("https://api.aliyun.com/api/product/makeCode")
            .post("{}".toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        val demoSdk = javaSdkCompletionContributor.getDemoSdk(project, okHttpClient, postRequest, "java")

        assertEquals("SDK 示例生成出错，请联系支持群开发同学解决", demoSdk)
    }

    @Test
    fun `getDemoSdk should handle IOException`() {
        `when`(call.execute()).thenThrow(IOException::class.java)

        val postRequest = Request.Builder().url("https://api.aliyun.com/api/product/makeCode")
            .post("{}".toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        val demoSdk = javaSdkCompletionContributor.getDemoSdk(project, okHttpClient, postRequest, "java")

        verify(normalNotification).showMessage(
            eq(project),
            eq(NotificationGroups.COMPLETION_NOTIFICATION_GROUP),
            eq("生成示例代码失败"),
            eq("请检查网络"),
            eq(NotificationType.WARNING)
        )

        assertEquals(String(), demoSdk)
    }
}
