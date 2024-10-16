package com.alibabacloud.api.service.util

import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.application.ApplicationNamesInfo
import com.intellij.openapi.extensions.PluginId
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.MockedStatic
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.whenever

internal class RequestUtilTest {
    private lateinit var pluginManagerCoreMock: MockedStatic<PluginManagerCore>
    private lateinit var applicationNamesInfoMock: MockedStatic<ApplicationNamesInfo>
    private lateinit var applicationInfoMock: MockedStatic<ApplicationInfo>
    private lateinit var osName: String
    private lateinit var osArch: String

    @BeforeEach
    fun setUp() {
        pluginManagerCoreMock = mockStatic(PluginManagerCore::class.java)
        applicationNamesInfoMock = mockStatic(ApplicationNamesInfo::class.java)
        applicationInfoMock = mockStatic(ApplicationInfo::class.java)
        osName = System.getProperties().getProperty("os.name")
        osArch = System.getProperties().getProperty("os.arch")

        System.setProperty("os.name", "Mac OS X")
        System.setProperty("os.arch", "aarch64")
        mockPluginInformation()
    }

    @AfterEach
    fun tearDown() {
        pluginManagerCoreMock.close()
        applicationNamesInfoMock.close()
        applicationInfoMock.close()
        System.setProperty("os.name", osName)
        System.setProperty("os.arch", osArch)
    }

    @Test
    fun `test GET Request is created correctly`() {
        val url = "https://example.com"
        val request = RequestUtil.createRequest(url)

        assertEquals("GET", request.method)
        assertEquals("$url/", request.url.toString())
        assertNull(request.header("user-agent"))
    }

    @Test
    fun `test POST Request is created correctly with body`() {
        val url = "https://example.com"
        val mediaType = "application/json; charset=utf-8".toMediaType();
        val requestBody = "{'key':'value'}".toRequestBody(mediaType)
        val headers = hashMapOf(
            "user-agent" to "Toolkit (Mac OS X; aarch64) alibabacloud-developer-toolkit/0.0.1 JetBrains/2021.1/IntelliJ IDEA"
        )
        val request = RequestUtil.createRequest(url, "POST", requestBody, headers)

        assertEquals("POST", request.method)
        assertEquals("$url/", request.url.toString())
        assertNotNull(request.body)
        assertEquals(
            "Toolkit (Mac OS X; aarch64) alibabacloud-developer-toolkit/0.0.1 JetBrains/2021.1/IntelliJ IDEA",
            request.header("user-agent")
        )
    }

    @Test
    fun `test Telemetry Request is created correctly with body`() {
        val url = "https://example.com"
        val mediaType = "application/json; charset=utf-8".toMediaType();
        val requestBody = "{'key':'value'}".toRequestBody(mediaType)
        val headers = hashMapOf(
            "user-agent" to "Toolkit (Mac OS X; aarch64) alibabacloud-developer-toolkit/0.0.1 JetBrains/2021.1/IntelliJ IDEA",
            "x-plugin-source-ip" to "1a:2b:34:5c:6d:b7",
            "x-plugin-timestamp" to "1722932858124",
            "x-plugin-token" to "1234567890abcdefg"
        )

        val request = RequestUtil.createRequest(url, "POST", requestBody, headers)

        assertEquals("POST", request.method)
        assertEquals("$url/", request.url.toString())
        assertNotNull(request.body)
        assertEquals(
            "1a:2b:34:5c:6d:b7",
            request.header("x-plugin-source-ip")
        )
        assertEquals(
            "1722932858124",
            request.header("x-plugin-timestamp")
        )
        assertEquals(
            "1234567890abcdefg",
            request.header("x-plugin-token")
        )
        assertEquals(
            "Toolkit (Mac OS X; aarch64) alibabacloud-developer-toolkit/0.0.1 JetBrains/2021.1/IntelliJ IDEA",
            request.header("user-agent")
        )
    }

    private fun mockPluginInformation() {
        val pd = mock<IdeaPluginDescriptor>()
        pluginManagerCoreMock.`when`<IdeaPluginDescriptor> { PluginManagerCore.getPlugin(PluginId.getId("alibabacloud.developer.toolkit")) }
            .thenReturn(pd)
        whenever(pd.version).thenReturn("0.0.1")

        val appNamesInfo = mock<ApplicationNamesInfo>()
        applicationNamesInfoMock.`when`<ApplicationNamesInfo> { ApplicationNamesInfo.getInstance() }
            .thenReturn(appNamesInfo)
        whenever(appNamesInfo.fullProductName).thenReturn("IntelliJ IDEA")

        val appInfo = mock<ApplicationInfo>()
        applicationInfoMock.`when`<ApplicationInfo> { ApplicationInfo.getInstance() }.thenReturn(appInfo)
        whenever(appInfo.fullVersion).thenReturn("2021.1")
    }

}