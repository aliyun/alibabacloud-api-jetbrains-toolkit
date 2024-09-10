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
    private var enableAKInspection: JBCheckBox = JBCheckBox()

    private fun saveBaseSettings() {
        val settingsState = ToolkitSettingsState.getInstance()
        settingsState.isAutoUpdateEnabled = enableToolkitAutoUpdate.isSelected
        settingsState.isCompletionEnabled = enableCompletion.isSelected
        settingsState.isAKInspectionEnabled = enableAKInspection.isSelected
    }


    override fun createComponent(): JComponent = panel {
        group(I18nUtils.getMsg("plugin.auto.update.settings")) {
            row {
                cell(enableToolkitAutoUpdate).applyToComponent {
                    this.isSelected = ToolkitSettingsState.getInstance().isAutoUpdateEnabled
                }
                text(I18nUtils.getMsg("plugin.auto.update.yes"))
            }
        }
        group(I18nUtils.getMsg("code.completion.settings")) {
            row {
                cell(enableCompletion).applyToComponent {
                    this.isSelected = ToolkitSettingsState.getInstance().isCompletionEnabled
                }
                text(I18nUtils.getMsg("settings.code.completion.enable"))
            }
        }
        group(I18nUtils.getMsg("settings.inspections.ak")) {
            row {
                cell(enableAKInspection).applyToComponent {
                    this.isSelected = ToolkitSettingsState.getInstance().isAKInspectionEnabled
                }
                text(
                    "${I18nUtils.getMsg("inspections.ak.enable")}<br/><br/>" +
                            "<font color='gray' size='-1'>${I18nUtils.getMsg("inspections.ak.recommend")}</font><br/>" +
                            "<font color='gray' size='-1'>${I18nUtils.getMsg("inspections.ak.no.save")}</font><br/>"
                )
            }
        }
        group(I18nUtils.getMsg("settings.feedback")) {
            row {
                text("<a>${I18nUtils.getMsg("settings.feedback.link")}</a>") {
                    BrowserUtil.browse(ExperienceQuestionnaire.QUESTIONNAIRE_LINK)
                }
            }
        }
    }

    override fun isModified(): Boolean {
        return enableToolkitAutoUpdate.isSelected != ToolkitSettingsState.getInstance().isAutoUpdateEnabled ||
                enableCompletion.isSelected != ToolkitSettingsState.getInstance().isCompletionEnabled ||
                enableAKInspection.isSelected != ToolkitSettingsState.getInstance().isAKInspectionEnabled
    }

    override fun apply() {
        saveBaseSettings()
    }

    override fun reset() {
        val settingsState = ToolkitSettingsState.getInstance()
        enableToolkitAutoUpdate.isSelected = settingsState.isAutoUpdateEnabled
        enableCompletion.isSelected = settingsState.isCompletionEnabled
        enableAKInspection.isSelected = settingsState.isAKInspectionEnabled
    }

    override fun getDisplayName(): String {
        return "Alibaba Cloud Developer Toolkit"
    }

    override fun getId(): String {
        return ToolkitInfo.PLUGIN_ID
    }
}