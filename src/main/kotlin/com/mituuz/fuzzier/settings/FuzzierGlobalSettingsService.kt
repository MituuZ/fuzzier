package com.mituuz.fuzzier.settings

import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.mituuz.fuzzier.entities.FuzzyContainer.FilenameType
import com.mituuz.fuzzier.entities.FuzzyContainer.FilenameType.FILE_PATH_ONLY

@State(
    name = "com.mituuz.fuzzier.FuzzierGlobalSettings",
    storages = [Storage("FuzzierGlobalSettings.xml")],
    reloadable = true
)
@Service(Service.Level.APP)
class FuzzierGlobalSettingsService : PersistentStateComponent<FuzzierGlobalSettingsService.State> {
    class State {
        var splitPosition: Int = 300

        var recentFilesMode: RecentFilesMode = RecentFilesMode.RECENT_PROJECT_FILES
        var filenameType: FilenameType = FILE_PATH_ONLY
        var highlightFilename = false
        var fileListFontSize = 14
        var previewFontSize = 0
        var fileListSpacing = 0

        var newTab: Boolean = false
        var prioritizeShorterDirPaths = true
        var debouncePeriod: Int = 80
        var resetWindow = false
        var fileListLimit: Int = 50

        var tolerance = 0
        var multiMatch = false
        var matchWeightPartialPath = 10
        var matchWeightSingleChar = 5
        var matchWeightStreakModifier = 10
        var matchWeightFilename = 10
    }

    private var state = State()

    override fun getState(): State {
        return this.state
    }

    override fun loadState(p0: State) {
        this.state = p0
    }

    enum class RecentFilesMode(val text: String) {
        NONE("None"),
        RECENT_PROJECT_FILES("Recent project files"),
        RECENTLY_SEARCHED_FILES("Recently searched files")
    }
}