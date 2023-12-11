package com.alibabacloud.credentials.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

@State(
    name = "com.example.demo.ToolkitSettingsState",
    storages = [Storage("alibabacloud.xml")],
)
class ToolkitSettingsState : PersistentStateComponent<ToolkitSettingsState?> {
    var profileName = ""
    var AccessKeyId = ""
    var AccessKeySecret = ""
    var memoryStatus = false

    override fun getState(): ToolkitSettingsState {
        return this
    }

    override fun loadState(state: ToolkitSettingsState) {
        XmlSerializerUtil.copyBean(state, this)
    }

    companion object {
        val instance: ToolkitSettingsState
            get() = ApplicationManager.getApplication().getService(ToolkitSettingsState::class.java)
    }

    fun saveConfig(profileName: String, accessKeyId: String, accessKeySecret: String, memoryStatus: Boolean) {
        this.profileName = profileName
        this.AccessKeyId = accessKeyId
        this.AccessKeySecret = accessKeySecret
        this.memoryStatus = memoryStatus
    }
}
