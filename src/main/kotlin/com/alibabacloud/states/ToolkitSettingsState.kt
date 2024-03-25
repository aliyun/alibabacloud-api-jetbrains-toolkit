package com.alibabacloud.states

import com.intellij.openapi.components.*

@Service(Service.Level.APP)
@State(
    name = "com.alibabacloud.states.ToolkitSettingsState",
    storages = [Storage("alibabacloud-developer-toolkit-settings.xml")]
)
class ToolkitSettingsState : PersistentStateComponent<ToolkitSettingsState.State> {
    data class State(
        var isCompletionEnabled: Boolean = true
    )

    private var toolkitState = State()

    override fun getState(): State = toolkitState

    override fun loadState(state: State) {
        toolkitState = state
    }

    fun setCompletion(isCompletionEnabled: Boolean) {
        toolkitState.isCompletionEnabled = isCompletionEnabled
    }

    fun isCompletionEnabled(): Boolean {
        return toolkitState.isCompletionEnabled
    }

    companion object {
        fun getInstance(): ToolkitSettingsState {
            return service()
        }
    }
}