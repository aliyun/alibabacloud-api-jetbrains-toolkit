package com.alibabacloud.action

import com.alibabacloud.states.ToolkitSettingsState
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent


class CodeSnippetsAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val settings = ToolkitSettingsState.getInstance()
        settings.setCompletion(!settings.isCompletionEnabled())
    }

    override fun update(e: AnActionEvent) {
        super.update(e)
        val project = e.project
        if (project == null) {
            e.presentation.isEnabledAndVisible = false
            return
        }
        val settings = ToolkitSettingsState.getInstance()
        if (settings.state.isCompletionEnabled) {
            e.presentation.text = "Disable Alibaba Cloud SDK code auto-completion"
        } else {
            e.presentation.text = "Enable Alibaba Cloud SDK code auto-completion"
        }
    }
}