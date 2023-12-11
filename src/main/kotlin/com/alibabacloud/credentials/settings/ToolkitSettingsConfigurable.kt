package com.alibabacloud.credentials.settings

import com.intellij.openapi.options.Configurable
import org.jetbrains.annotations.Nls
import javax.swing.JComponent

class ToolkitSettingsConfigurable : Configurable {
    private var mySettingsComponent: ToolkitSettingsComponent? = null

    override fun getDisplayName(): @Nls(capitalization = Nls.Capitalization.Title) String {
        return "Alibaba Cloud Credentials Settings"
    }

    override fun getPreferredFocusedComponent(): JComponent {
        return mySettingsComponent!!.preferredFocusedComponent
    }

    override fun createComponent(): JComponent {
        mySettingsComponent = ToolkitSettingsComponent()
        return mySettingsComponent!!.panel
    }

    override fun isModified(): Boolean {
        val settings: ToolkitSettingsState = ToolkitSettingsState.instance
        var modified = mySettingsComponent!!.userProfileName != settings.profileName
        modified = modified or (mySettingsComponent!!.userAccessKeyId != settings.AccessKeyId)
        modified = modified or (mySettingsComponent!!.userAccessKeySecret != settings.AccessKeySecret)
        modified = modified or (mySettingsComponent!!.memoryStatus != settings.memoryStatus)
        return modified
    }

    override fun apply() {
        val settings: ToolkitSettingsState = ToolkitSettingsState.instance
        settings.profileName = mySettingsComponent!!.userProfileName!!
        settings.AccessKeyId = mySettingsComponent!!.userAccessKeyId!!
        settings.AccessKeySecret = mySettingsComponent!!.userAccessKeySecret!!
        settings.memoryStatus = mySettingsComponent!!.memoryStatus
        ToolkitSettingsState.instance.saveConfig(
            settings.profileName,
            settings.AccessKeyId,
            settings.AccessKeySecret,
            settings.memoryStatus,
        )
    }

    override fun reset() {
        val settings: ToolkitSettingsState = ToolkitSettingsState.instance
        mySettingsComponent!!.userProfileName = settings.profileName
        mySettingsComponent!!.userAccessKeyId = settings.AccessKeyId
        mySettingsComponent!!.userAccessKeySecret = settings.AccessKeySecret
        mySettingsComponent!!.memoryStatus = settings.memoryStatus
    }

    override fun disposeUIResources() {
        mySettingsComponent = null
    }
}
