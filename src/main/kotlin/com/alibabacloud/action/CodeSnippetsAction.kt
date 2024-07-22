package com.alibabacloud.action

import com.alibabacloud.i18n.I18nUtils
import com.alibabacloud.states.ToolkitSettingsState
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent


class CodeSnippetsAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        ToolkitSettingsState.getInstance().isCompletionEnabled = !ToolkitSettingsState.getInstance().isCompletionEnabled
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
            e.presentation.text = I18nUtils.getMsg("code.completion.disable")
        } else {
            e.presentation.text = I18nUtils.getMsg("code.completion.enable")
        }
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }
}