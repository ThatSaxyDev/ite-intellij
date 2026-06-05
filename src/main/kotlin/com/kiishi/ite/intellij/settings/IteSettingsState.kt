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
        var resumeLastSession: Boolean = true,
    )

    private var state = State()

    override fun getState(): State = state

    override fun loadState(state: State) {
        this.state = state
    }

    companion object {
        fun getInstance(): IteSettingsState = service()
    }
}
