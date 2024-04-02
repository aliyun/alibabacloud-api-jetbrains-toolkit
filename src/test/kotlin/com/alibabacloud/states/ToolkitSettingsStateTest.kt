package com.alibabacloud.states

import com.intellij.openapi.components.service
import com.intellij.testFramework.fixtures.BasePlatformTestCase

internal class ToolkitSettingsStateTest : BasePlatformTestCase() {
    override fun setUp() {
        super.setUp()
        val settingsState = service<ToolkitSettingsState>()
        settingsState.setCompletion(true)
    }

    override fun tearDown() {
        super.tearDown()
        val settingsState = service<ToolkitSettingsState>()
        settingsState.setCompletion(true)
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

        settingsState.setCompletion(false)
        assertFalse(settingsState.isCompletionEnabled())

        settingsState.setCompletion(true)
        assertTrue(settingsState.isCompletionEnabled())
    }
}