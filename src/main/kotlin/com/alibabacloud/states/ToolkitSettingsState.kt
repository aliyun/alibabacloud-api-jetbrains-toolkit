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
        var isAutoUpdateEnabled: Boolean = true
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


    companion object {
        fun getInstance(): ToolkitSettingsState {
            return service()
        }
    }
}

interface ToolkitSettings {
    var isCompletionEnabled: Boolean
    var isAutoUpdateEnabled: Boolean
}