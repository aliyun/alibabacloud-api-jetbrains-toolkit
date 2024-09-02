package com.alibabacloud.settings

import com.alibabacloud.states.ToolkitSettingsState
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
        group("自动更新设置") {
            row {
                cell(enableToolkitAutoUpdate).applyToComponent {
                    this.isSelected = ToolkitSettingsState.getInstance().isAutoUpdateEnabled
                }
                text("自动检查并更新插件版本")
            }
        }
        group("代码补全设置") {
            row {
                cell(enableCompletion).applyToComponent {
                    this.isSelected = ToolkitSettingsState.getInstance().isCompletionEnabled
                }
                text("自动插入 SDK 示例代码（或通过快捷键 ctrl + cmd + p 切换）")
            }
        }
        group("明文AK/SK检查设置") {
            row {
                cell(enableAKInspection).applyToComponent {
                    this.isSelected = ToolkitSettingsState.getInstance().isAKInspectionEnabled
                }
                text(
                    "开启明文AK/SK检查（强烈建议保持开启）<br/><br/>" +
                            "<font color='gray' size='-1'>阿里云主账号拥有资源的全部权限，AK一旦泄露，会给系统带来巨大风险，不建议通过明文使用。</font><br/>" +
                            "<font color='gray' size='-1'>我们只通过正则匹配您代码中的明文AK/SK，不会进行任何保存/上传。</font><br/>"
                )
            }
        }
        group("用户反馈") {
            row {
                text("<a>反馈链接</a>") {
                    BrowserUtil.browse("https://g.alicdn.com/aes/tracker-survey-preview/0.0.13/survey.html?pid=fePxMy&id=3494")
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