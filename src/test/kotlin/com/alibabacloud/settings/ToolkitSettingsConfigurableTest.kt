package com.alibabacloud.settings

import com.intellij.testFramework.ProjectRule
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ToolkitSettingsConfigurableTest {
    @JvmField
    @Rule
    val projectRule = ProjectRule()

    private lateinit var configurable: ToolkitSettingsConfigurable

    @Before
    fun setUp() {
        configurable = ToolkitSettingsConfigurable()
    }

    @Test
    fun `test no operation should ok`() {
        configurable.apply()
    }

    @Test
    fun `test change toolkit auto update state should ok`() {
        configurable.enableToolkitAutoUpdate.isSelected = true
        configurable.apply()
        configurable.enableToolkitAutoUpdate.isSelected = false
        configurable.apply()
    }

    @Test
    fun `test change SDK code auto completion state should ok`() {
        configurable.enableCompletion.isSelected = true
        configurable.apply()
        configurable.enableCompletion.isSelected = false
        configurable.apply()
    }

    @Test
    fun `test change AK inspections state should ok`() {
        val cacheSelected = configurable.enableAKInspection.isSelected
        configurable.enableAKInspection.isSelected = true
        configurable.apply()
        assertEquals(true, configurable.enableAKInspection.isSelected)
        configurable.enableAKInspection.isSelected = cacheSelected
        configurable.apply()
    }

    @Test
    fun `test change telemetry data state should ok`() {
        val cacheSelected = configurable.enableTelemetry.isSelected
        assertEquals(false, configurable.enableTelemetry.isSelected)
        configurable.enableTelemetry.isSelected = true
        configurable.apply()
        assertEquals(true, configurable.enableTelemetry.isSelected)
        configurable.enableTelemetry.isSelected = cacheSelected
        configurable.apply()
    }
}