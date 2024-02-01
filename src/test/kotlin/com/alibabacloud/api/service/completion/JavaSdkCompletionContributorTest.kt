package com.alibabacloud.api.service.completion

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

internal class JavaSdkCompletionContributorTest : BasePlatformTestCase() {
    fun testDemoSdk() {
        val jsonString = """
        {
            "apiName": "ListReservedCapacities",
            "apiVersion": "2021-04-06",
            "product": "FC-Open",
            "sdkType": "dara",
            "params": {},
            "simplify": true
        }
    """.trimIndent()
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = jsonString.toRequestBody(mediaType)

        val request = Request.Builder()
            .url("https://api.aliyun.com/api/product/makeCode").post(body)
            .build()

        val javaSdkCompletionContributor = JavaSdkCompletionContributor()
        val method = JavaSdkCompletionContributor::class.java.getDeclaredMethod(
            "getDemoSdk",
            Request::class.java,
            String::class.java
        )
        method.isAccessible = true
        val args = arrayOfNulls<Any>(2)
        args[0] = request
        args[1] = "java"
        val demoSdk = method.invoke(javaSdkCompletionContributor, *args)
        val expectedCodeSample =
            "com.aliyun.teaopenapi.models.Config config = new com.aliyun.teaopenapi.models.Config()\n        // 请确保代码运行环境设置了环境变量 ALIBABA_CLOUD_ACCESS_KEY_ID 和 ALIBABA_CLOUD_ACCESS_KEY_SECRET。\n        .setAccessKeyId(System.getenv(\"ALIBABA_CLOUD_ACCESS_KEY_ID\"))\n        .setAccessKeySecret(System.getenv(\"ALIBABA_CLOUD_ACCESS_KEY_SECRET\"))\n        // Endpoint 请参考 https://api.aliyun.com/product/FC-Open\n        .setEndpoint(\"<your-account-id>.<region>.fc.aliyuncs.com\");\ncom.aliyun.fc_open20210406.Client client = new com.aliyun.fc_open20210406.Client(config);\ncom.aliyun.fc_open20210406.models.ListReservedCapacitiesHeaders listReservedCapacitiesHeaders = new com.aliyun.fc_open20210406.models.ListReservedCapacitiesHeaders();\ncom.aliyun.fc_open20210406.models.ListReservedCapacitiesRequest listReservedCapacitiesRequest = new com.aliyun.fc_open20210406.models.ListReservedCapacitiesRequest();\ncom.aliyun.fc_open20210406.models.ListReservedCapacitiesResponse listReservedCapacitiesResponse = client.listReservedCapacities(listReservedCapacitiesRequest, listReservedCapacitiesHeaders);"
        assertEquals(expectedCodeSample, demoSdk)
    }
}
