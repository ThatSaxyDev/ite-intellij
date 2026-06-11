package com.kiishi.ite.intellij.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service

@Service(Service.Level.APP)
@State(name = "IteSettings", storages = [Storage("ite.xml")])
class IteSettingsState : PersistentStateComponent<IteSettingsState.State> {
    data class State(
        var executable: String = "ite",
        var args: MutableList<String> = mutableListOf(),
        var resumePromptMode: String = PROMPT_MODE_ASK,
        @Deprecated("Use resumePromptMode instead.")
        var resumeLastSession: Boolean = true,
    )

    private var state = State()

    override fun getState(): State = state

    override fun loadState(state: State) {
        this.state = migrate(state)
    }

    private fun migrate(loaded: State): State {
        val mode = loaded.resumePromptMode
        val valid = mode == PROMPT_MODE_ASK || mode == PROMPT_MODE_AUTO || mode == PROMPT_MODE_NEVER
        if (!valid) {
            loaded.resumePromptMode = if (!loaded.resumeLastSession) PROMPT_MODE_NEVER else PROMPT_MODE_ASK
        }
        return loaded
    }

    companion object {
        const val PROMPT_MODE_ASK = "ask"
        const val PROMPT_MODE_AUTO = "auto"
        const val PROMPT_MODE_NEVER = "never"

        fun getInstance(): IteSettingsState = service()

        fun effectiveResumePromptMode(): String {
            val state = getInstance().state
            val mode = state.resumePromptMode
            return when (mode) {
                PROMPT_MODE_ASK, PROMPT_MODE_AUTO, PROMPT_MODE_NEVER -> mode
                else -> PROMPT_MODE_ASK
            }
        }
    }
}
