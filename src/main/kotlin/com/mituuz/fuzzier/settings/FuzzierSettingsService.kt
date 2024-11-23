/*
MIT License

Copyright (c) 2024 Mitja Leino

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/
package com.mituuz.fuzzier.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.mituuz.fuzzier.entities.FuzzyMatchContainer
import com.mituuz.fuzzier.entities.FuzzyMatchContainer.FilenameType
import com.mituuz.fuzzier.entities.FuzzyMatchContainer.FilenameType.FILE_PATH_ONLY
import com.mituuz.fuzzier.settings.FuzzierSettingsService.RecentFilesMode.RECENT_PROJECT_FILES
import javax.swing.DefaultListModel

@State(
    name = "com.mituuz.fuzzier.FuzzierSettings",
    storages = [Storage("FuzzierSettings.xml")]
)
class FuzzierSettingsService : PersistentStateComponent<FuzzierSettingsService.State> {
    class State {
        var modules: Map<String, String> = HashMap()
        var isProject = false
        var recentFilesMode: RecentFilesMode = RECENT_PROJECT_FILES
        var recentlySearchedFiles: DefaultListModel<FuzzyMatchContainer> = DefaultListModel()

        var splitPosition: Int = 300
        var exclusionSet: Set<String> = setOf("/.idea/*", "/.git/*", "/target/*", "/build/*", "/.gradle/*", "/.run/*")
        var newTab: Boolean = false
        var prioritizeShorterDirPaths = true
        var debouncePeriod: Int = 80
        var resetWindow = false
        var fileListLimit: Int = 50

        var filenameType: FilenameType = FILE_PATH_ONLY
        var highlightFilename = false
        var fileListFontSize = 14
        var previewFontSize = 0
        var fileListSpacing = 0

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