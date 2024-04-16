package com.alibabacloud.api.service.completion

import com.alibabacloud.api.service.completion.java.JavaSdkCompletionContributor
import com.alibabacloud.api.service.constants.NotificationGroups
import com.alibabacloud.api.service.notification.NormalNotification
import com.google.gson.JsonArray
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.Application
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Computable
import com.intellij.openapi.util.IconLoader
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import java.io.IOException

@ExtendWith(MockitoExtension::class)
internal class SdkCompletionContributorTest {
    @Mock
    lateinit var project: Project

    @Mock
    lateinit var document: Document

    @Mock
    lateinit var application: Application

    private lateinit var applicationManagerStaticMock: MockedStatic<ApplicationManager>

    @Mock
    lateinit var psiDocumentManager: PsiDocumentManager

    @Mock
    lateinit var psiFile: PsiFile

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
                    },
                    "python": {
                        "codeSample": "Python Code Sample.",
                            "importList": [
                                "from alibabacloud_tea_openapi import models as open_api_models"
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

    private val sampleResponse1: String = """
        {
            "code": 0,
            "data": {
                "demoSdk": {
                    "python": {
                        "codeSample": "Python Code Sample.",
                            "importList": [
                                "from alibabacloud_tea_openapi import models as open_api_models"
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
        applicationManagerStaticMock = Mockito.mockStatic(ApplicationManager::class.java)
        applicationManagerStaticMock.`when`<Any> { ApplicationManager.getApplication() }.thenReturn(application)
        javaSdkCompletionContributor.notificationService = normalNotification
    }

    @AfterEach
    fun tearDown() {
        javaSdkCompletionContributor.notificationService = NormalNotification
        applicationManagerStaticMock.close()
    }

    @Test
    fun `getDemoSdk should return correct sample code on successful response`() {
        `when`(okHttpClient.newCall(any())).thenReturn(call)
        `when`(call.execute()).thenReturn(response)
        `when`(application.runReadAction<String>(any())).thenAnswer {
            (it.arguments[0] as Computable<*>).compute()
        }
        `when`(PsiDocumentManager.getInstance(project)).thenReturn(psiDocumentManager)
        `when`(psiDocumentManager.getCachedPsiFile(document)).thenReturn(psiFile)

        `when`(response.isSuccessful).thenReturn(true)
        `when`(response.body).thenReturn(responseBody)
        `when`(responseBody.string()).thenReturn(sampleResponse)

        val postRequest = Request.Builder().url("https://api.aliyun.com/api/product/makeCode")
            .post("{}".toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        val javaInfo = javaSdkCompletionContributor.getDemoSdk(project, document, okHttpClient, postRequest, "java")
        val demoSdkJava = javaInfo[0].asString
        val javaAsyncInfo =
            javaSdkCompletionContributor.getDemoSdk(project, document, okHttpClient, postRequest, "java-async")
        val demoSdkJavaAsync = javaAsyncInfo[0].asString
        assertEquals("Java Code Sample.", demoSdkJava)
        assertEquals("Java Async Code Sample.", demoSdkJavaAsync)

        `when`(responseBody.string()).thenReturn(sampleResponse1)
        val postRequest1 = Request.Builder().url("https://api.aliyun.com/api/product/makeCode")
            .post("{}".toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        val javaInfo1 = javaSdkCompletionContributor.getDemoSdk(project, document, okHttpClient, postRequest1, "java")
        val demoSdkJava1 = javaInfo1[0].asString
        assertEquals("// 该 API 暂无 java SDK 示例", demoSdkJava1)

    }

    @Test
    fun `getDemoSdk should handle unsuccessful response`() {
        `when`(okHttpClient.newCall(any())).thenReturn(call)
        `when`(call.execute()).thenReturn(response)
        `when`(application.runReadAction<String>(any())).thenAnswer {
            (it.arguments[0] as Computable<*>).compute()
        }
        `when`(PsiDocumentManager.getInstance(project)).thenReturn(psiDocumentManager)
        `when`(psiDocumentManager.getCachedPsiFile(document)).thenReturn(psiFile)

        val mockFileType = mock(FileType::class.java)
        `when`(mockFileType.name).thenReturn("JAVA")
        `when`(psiFile.fileType).thenReturn(mockFileType)

        `when`(response.isSuccessful).thenReturn(false)

        val postRequest = Request.Builder().url("https://api.aliyun.com/api/product/makeCode")
            .post("{}".toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        val demoSdk =
            javaSdkCompletionContributor.getDemoSdk(project, document, okHttpClient, postRequest, "java")[0].asString
        assertEquals("// SDK 示例生成出错，请联系支持群开发同学解决", demoSdk)
    }

    @Test
    fun `getDemoSdk should handle IOException`() {
        `when`(okHttpClient.newCall(any())).thenReturn(call)
        `when`(call.execute()).thenReturn(response)
        `when`(call.execute()).thenThrow(IOException::class.java)

        val postRequest = Request.Builder().url("https://api.aliyun.com/api/product/makeCode")
            .post("{}".toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        val demoSdk =
            javaSdkCompletionContributor.getDemoSdk(project, document, okHttpClient, postRequest, "java")[0].asString

        verify(normalNotification).showMessage(
            eq(project),
            eq(NotificationGroups.COMPLETION_NOTIFICATION_GROUP),
            eq("生成示例代码失败"),
            eq("请检查网络"),
            eq(NotificationType.WARNING)
        )

        assertEquals(String(), demoSdk)
    }

    @Test
    fun `addElements should be called with proper parameters`() {
        val completionResultSetMock = mock(CompletionResultSet::class.java)
        val documentMock = mock(Document::class.java)
        val requestMock = mock(Request::class.java)

        val sdkCompletionContributorMock = mock(SdkCompletionContributor::class.java)

        Mockito.doNothing().`when`(sdkCompletionContributorMock)
            .addElements(any(), anyString(), anyString(), any(), any())

        sdkCompletionContributorMock.addElements(
            completionResultSetMock, "key", "value", documentMock, requestMock
        )

        verify(sdkCompletionContributorMock).addElements(
            completionResultSetMock, "key", "value", documentMock, requestMock
        )
    }
}
