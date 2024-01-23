package com.alibabacloud.models.credentials

import com.intellij.testFramework.fixtures.BasePlatformTestCase

internal class ConfigureFileTest : BasePlatformTestCase() {
    fun testLoadConfigureFile() {
        val configure = ConfigureFile.loadConfigureFile(javaClass.classLoader.getResource("aliyun_test.json").toString())
        if (configure != null) {
            assertEquals("", configure.current)
        }

    }

    fun testGetDefaultPath() {
        assertTrue(ConfigureFile.getDefaultPath().endsWith(".aliyun/config.json"))
    }
}