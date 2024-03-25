package com.alibabacloud.api.service.util

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

object ResourceUtil {
    fun load(resourcePath: String): String {
        return readAllBytes(ResourceUtil::class.java.getResourceAsStream(resourcePath))
    }

    fun loadPath(resourcePath: String): String {
        return ResourceUtil::class.java.getResource(resourcePath)!!.toString()
    }

    private fun readAllBytes(inputStream: InputStream?): String {
        try {
            val reader = BufferedReader(InputStreamReader(inputStream!!, StandardCharsets.UTF_8))
            return try {
                val sb = StringBuilder()
                var line = reader.readLine()
                while (line != null) {
                    sb.append(line)
                    sb.append(System.lineSeparator())
                    line = reader.readLine()
                }
                val str1 = sb.toString()
                reader.close()
                str1
            } catch (throwable: Throwable) {
                try {
                    reader.close()
                } catch (throwable1: Throwable) {
                    throwable.addSuppressed(throwable1)
                }
                throw throwable
            }
        } catch (_: IOException) {
        }
        return ""
    }
}
