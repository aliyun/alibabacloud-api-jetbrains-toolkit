package com.alibabacloud.settings

import com.alibabacloud.i18n.I18nUtils
import com.alibabacloud.states.ToolkitSettingsState
import com.alibabacloud.telemetry.ExperienceQuestionnaire
import com.alibabacloud.toolkit.ToolkitInfo
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

class ToolkitSettingsConfigurable : SearchableConfigurable {
    var enableToolkitAutoUpdate: JBCheckBox = JBCheckBox()
    var enableCompletion: JBCheckBox = JBCheckBox()

    private fun saveBaseSettings() {
        val settingsState = ToolkitSettingsState.getInstance()
        settingsState.isAutoUpdateEnabled = enableToolkitAutoUpdate.isSelected
        settingsState.isCompletionEnabled= enableCompletion.isSelected
    }


    override fun createComponent(): JComponent = panel {
        group(I18nUtils.getMsg("AUTO_UPDATE_SETTINGS")) {
            row {
                cell(enableToolkitAutoUpdate).applyToComponent {
                    this.isSelected = ToolkitSettingsState.getInstance().isAutoUpdateEnabled
                }
                text(I18nUtils.getMsg("AUTO_CHECK_AND_UPDATE_PLUGIN"))
            }
        }
        group(I18nUtils.getMsg("CODE_COMPLETION_SETTINGS")) {
            row {
                cell(enableCompletion).applyToComponent {
                    this.isSelected = ToolkitSettingsState.getInstance().isCompletionEnabled
                }
                text(I18nUtils.getMsg("AUTO_COMPLETION"))
            }
        }
        group(I18nUtils.getMsg("CUSTOMER_FEEDBACK")) {
            row {
                text("<a>${I18nUtils.getMsg("FEEDBACK_LINK")}</a>") {
                    BrowserUtil.browse(ExperienceQuestionnaire.QUESTIONNAIRE_LINK)
                }
            }
        }
    }

    override fun isModified(): Boolean {
        return enableToolkitAutoUpdate.isSelected != ToolkitSettingsState.getInstance().isAutoUpdateEnabled ||
                enableCompletion.isSelected != ToolkitSettingsState.getInstance().isCompletionEnabled
    }

    override fun apply() {
        saveBaseSettings()
    }

    override fun reset() {
        val settingsState = ToolkitSettingsState.getInstance()
        enableToolkitAutoUpdate.isSelected = settingsState.isAutoUpdateEnabled
        enableCompletion.isSelected = settingsState.isCompletionEnabled
    }

    override fun getDisplayName(): String {
        return "Alibaba Cloud Developer Toolkit"
    }

    override fun getId(): String {
        return ToolkitInfo.PLUGIN_ID
    }
}