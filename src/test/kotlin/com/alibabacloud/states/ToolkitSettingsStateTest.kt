package com.alibabacloud.states

import com.intellij.openapi.components.service
import com.intellij.testFramework.fixtures.BasePlatformTestCase

internal class ToolkitSettingsStateTest : BasePlatformTestCase() {
    override fun setUp() {
        super.setUp()
        val settingsState = service<ToolkitSettingsState>()
        settingsState.isCompletionEnabled = true
    }

    override fun tearDown() {
        super.tearDown()
        val settingsState = service<ToolkitSettingsState>()
        settingsState.isCompletionEnabled = true
    }

    fun `test getState`() {
        val settingsState = service<ToolkitSettingsState>()
        assertNotNull(settingsState)

        val initialState = settingsState.state
        assertTrue(initialState.isCompletionEnabled)
    }

    fun `test setCompletion and isCompletionEnabled`() {
        val settingsState = service<ToolkitSettingsState>()
        assertNotNull(settingsState)

        settingsState.isCompletionEnabled =false
        assertFalse(settingsState.isCompletionEnabled)

        settingsState.isCompletionEnabled = true
        assertTrue(settingsState.isCompletionEnabled)
    }
}