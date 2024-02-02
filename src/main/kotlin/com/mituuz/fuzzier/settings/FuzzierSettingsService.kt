package com.mituuz.fuzzier.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@State(
    name = "com.mituuz.fuzzier.FuzzierSettings",
    storages = [Storage("FuzzierSettings.xml")]
)
class FuzzierSettingsService : PersistentStateComponent<FuzzierSettingsService.State> {
    class State {
        var splitPosition: Int = 300
        var exclusionList: List<String> = listOf("/.idea/", "/.git/", "/target/", "/build/", "/.gradle/", "/.run/")
        var newTab: Boolean = false
        var debouncePeriod: Int = 150
        var resetWindow = false
        var multiMatch = true
    }

    private var state = State()

    override fun getState(): State {
        return this.state
    }

    override fun loadState(p0: State) {
        this.state = p0
    }
}