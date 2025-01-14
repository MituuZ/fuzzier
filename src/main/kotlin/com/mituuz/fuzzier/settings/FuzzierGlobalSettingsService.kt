package com.mituuz.fuzzier.settings

import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.PersistentStateComponent

@State(
    name = "com.mituuz.fuzzier.FuzzierGlobalSettings",
    storages = [Storage("FuzzierGlobalSettings.xml")]
)
class FuzzierGlobalSettingsService : PersistentStateComponent<FuzzierGlobalSettingsService.State> {
    class State {

    }

    private var state = State()

    override fun getState(): State {
        return this.state
    }

    override fun loadState(p0: State) {
        this.state = p0
    }
}