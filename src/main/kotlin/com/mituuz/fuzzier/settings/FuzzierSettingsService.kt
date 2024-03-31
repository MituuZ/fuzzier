package com.mituuz.fuzzier.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.mituuz.fuzzier.entities.FuzzyMatchContainer.FilenameType
import com.mituuz.fuzzier.entities.FuzzyMatchContainer.FilenameType.FILEPATH_ONLY

@State(
    name = "com.mituuz.fuzzier.FuzzierSettings",
    storages = [Storage("FuzzierSettings.xml")]
)
class FuzzierSettingsService : PersistentStateComponent<FuzzierSettingsService.State> {
    class State {
        var splitPosition: Int = 300
        var exclusionSet: Set<String> = setOf("/.idea/*", "/.git/*", "/target/*", "/build/*", "/.gradle/*", "/.run/*")
        var newTab: Boolean = false
        var debouncePeriod: Int = 150
        var resetWindow = false
        var multiMatch = false
        var filenameType: FilenameType = FILEPATH_ONLY
        var fontSize = 14
        var fileListSpacing = 0

        var matchWeightPartialPath = 10
        var matchWeightSingleChar = 5
        var matchWeightStreakModifier = 10
    }

    private var state = State()

    override fun getState(): State {
        return this.state
    }

    override fun loadState(p0: State) {
        this.state = p0
    }
}