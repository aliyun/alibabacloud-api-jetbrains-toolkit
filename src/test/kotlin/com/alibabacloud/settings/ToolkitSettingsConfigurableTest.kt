package com.alibabacloud.settings

import com.intellij.testFramework.ProjectRule
import org.junit.Rule
import org.junit.Test

class ToolkitSettingsConfigurableTest {
    @JvmField
    @Rule
    val projectRule = ProjectRule()

    @Test
    fun `test no operation should ok`() {
        val settings = ToolkitSettingsConfigurable()
        settings.apply()
    }

    @Test
    fun `test change toolkit auto update state should ok`() {
        val configurable = ToolkitSettingsConfigurable()
        configurable.enableToolkitAutoUpdate.isSelected = true
        configurable.apply()
        configurable.enableToolkitAutoUpdate.isSelected = false
        configurable.apply()
    }

    @Test
    fun `test change SDK code auto completion state should ok`() {
        val configurable = ToolkitSettingsConfigurable()
        configurable.enableCompletion.isSelected = true
        configurable.apply()
        configurable.enableCompletion.isSelected = false
        configurable.apply()
    }
}