package com.alibabacloud.action

import com.alibabacloud.i18n.I18nUtils
import com.alibabacloud.states.ToolkitSettingsState
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.mockito.Mockito

internal class CodeSnippetsActionTest : BasePlatformTestCase() {
    private lateinit var action: CodeSnippetsAction
    private lateinit var anActionEvent: AnActionEvent
    private lateinit var presentation: Presentation

    override fun setUp() {
        super.setUp()

        action = CodeSnippetsAction()

        presentation = Presentation()

        anActionEvent = Mockito.mock(AnActionEvent::class.java)

        Mockito.`when`(anActionEvent.presentation).thenReturn(presentation)

        val mockProject = Mockito.mock(project::class.java)

        Mockito.`when`(anActionEvent.project).thenReturn(mockProject)
    }

    fun `test actionPerformed should toggle completion state`() {
        val settings = ToolkitSettingsState.getInstance()
        val initialState = settings.isCompletionEnabled

        action.actionPerformed(anActionEvent)
        val afterState = settings.isCompletionEnabled

        assertEquals(initialState, true)
        assertEquals(afterState, false)
    }

    fun `test update should change presentation text based on state`() {
        val settings = ToolkitSettingsState.getInstance()

        settings.isCompletionEnabled = true
        action.update(anActionEvent)
        assertEquals(I18nUtils.getMsg("code.completion.disable"), anActionEvent.presentation.text)

        settings.isCompletionEnabled = false
        action.update(anActionEvent)
        assertEquals(I18nUtils.getMsg("code.completion.enable"), anActionEvent.presentation.text)
    }
}