package com.alibabacloud.states

import com.intellij.openapi.components.*

@Service(Service.Level.APP)
@State(
    name = "com.alibabacloud.states.ToolkitSettingsState",
    storages = [Storage("alibabacloud-developer-toolkit-settings.xml")]
)
class ToolkitSettingsState : PersistentStateComponent<ToolkitSettingsState.State>, ToolkitSettings {
    data class State(
        var isCompletionEnabled: Boolean = true,
        var isAutoUpdateEnabled: Boolean = true,
        var isAKInspectionEnabled: Boolean = true
    )

    private var toolkitState = State()

    override fun getState(): State = toolkitState

    override fun loadState(state: State) {
        toolkitState = state
    }

    override var isCompletionEnabled: Boolean
        get() = state.isCompletionEnabled
        set(isCompletionEnabled) {
            toolkitState.isCompletionEnabled = isCompletionEnabled
        }

    override var isAutoUpdateEnabled: Boolean
        get() = state.isAutoUpdateEnabled
        set(isAutoUpdateEnabled) {
            toolkitState.isAutoUpdateEnabled = isAutoUpdateEnabled
        }

    override var isAKInspectionEnabled: Boolean
        get() = state.isAKInspectionEnabled
        set(isAKInspectionEnabled) {
            toolkitState.isAKInspectionEnabled = isAKInspectionEnabled
        }


    companion object {
        fun getInstance(): ToolkitSettingsState {
            return service()
        }
    }
}

interface ToolkitSettings {
    var isCompletionEnabled: Boolean
    var isAutoUpdateEnabled: Boolean
    var isAKInspectionEnabled: Boolean
}