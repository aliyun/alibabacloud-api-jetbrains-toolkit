package com.alibabacloud.api.service.sdksample.util

import com.alibabacloud.api.service.OkHttpClientProvider
import com.alibabacloud.api.service.completion.CompletionIndexPersistentComponent
import com.alibabacloud.api.service.completion.DataService
import com.alibabacloud.api.service.util.RequestUtil
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import java.io.IOException

class GenerateDocUtil {
    companion object {
        private fun getIndex(): Map<String, String> {
            val completionIndex = CompletionIndexPersistentComponent.getInstance()
            val state = completionIndex.state
            return if (state.completionIndex?.isNotEmpty() == true) {
                state.completionIndex!!
            } else {
                if (DataService.isDataLoaded()) DataService.javaIndex else emptyMap()
            }
        }

        private fun findMatchingKey(keyInfo: String?, index: Map<String, String>): String? {
            keyInfo ?: return null
            return index.keys.find { key ->
                val normalizedKey = key.replace("-", "").replace("_", "")
                val javaKey = normalizedKey.replace("::", "")
                val parts = normalizedKey.split("::")
                val pyKey = if (parts.size == 3) {
                    parts[1] + parts[2] + parts[0]
                } else {
                    normalizedKey
                }
                javaKey.equals(keyInfo, ignoreCase = true) || pyKey.equals(keyInfo, ignoreCase = true)
            }
        }

        private fun hasApiSamples(product: String, version: String, apiName: String): Boolean {
            val url = "https://api.aliyun.com/api/samples/product/${product}/version/${version}/api/${apiName}"
            val request = RequestUtil.createRequest(url)
            var data = JsonArray()
            try {
                OkHttpClientProvider.instance.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val resp = Gson().fromJson(response.body?.string(), JsonObject::class.java)
                        data = resp.get("data").asJsonArray
                    }
                }
            } catch (e: IOException) {
                return false
            }
            return data.size() > 0
        }

        internal fun generateProductDoc(productVersion: String): String? {
            val pattern = Regex("^([a-zA-Z_-]+)(\\d+)$")
            val matchResult = pattern.find(productVersion)
            if (matchResult != null) {
                val (product, _) = matchResult.destructured
                return "&nbsp;&nbsp;\uD83D\uDCA1 <a href=https://api.aliyun.com/api-tools/demo/${product}>查看更多「${product}」相关代码示例</a>"
            }
            return null
        }

        internal fun generateApiDoc(keyInfo: String?): String? {
            val index = getIndex()
            if (index.isNotEmpty()) {
                val matchingKey = findMatchingKey(keyInfo, index)
                if (matchingKey != null) {
                    val apiInfo = matchingKey.split("::")
                    val apiName = apiInfo[0]
                    val product = apiInfo[1]
                    val version = apiInfo[2]
                    return if (hasApiSamples(product, version, apiName)) {
                        "&nbsp;&nbsp;\uD83D\uDCA1 <a href=https://api.aliyun.com/api/${product}/${version}/${apiName}?tab=CodeSample>查看更多「${apiName}」相关代码示例</a><br><br>&nbsp;&nbsp;&nbsp;${index[matchingKey]}"
                    } else {
                        "&nbsp;&nbsp;\uD83D\uDCA1 <a href=https://api.aliyun.com/api-tools/demo/${product}>查看更多「${product}」相关代码示例</a><br><br>&nbsp;&nbsp;&nbsp;${index[matchingKey]}"
                    }
                }
            }
            return null
        }
    }
}