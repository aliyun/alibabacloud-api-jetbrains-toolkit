package com.alibabacloud.api.service.util

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows


internal class RequestUtilTest {
    @Test
    fun `test GET Request is created correctly`() {
        val url = "https://example.com"
        val request = RequestUtil.createRequest(url)

        assertEquals("GET", request.method)
        assertEquals("$url/", request.url.toString())
    }

    @Test
    fun `test POST Request is created correctly with body`() {
        val url = "https://example.com"
        val mediaType = "application/json; charset=utf-8".toMediaType();
        val requestBody = "{'key':'value'}".toRequestBody(mediaType)
        val request = RequestUtil.createRequest(url, "POST", requestBody)

        assertEquals("POST", request.method)
        assertEquals("$url/", request.url.toString())
        assertNotNull(request.body)
    }
}