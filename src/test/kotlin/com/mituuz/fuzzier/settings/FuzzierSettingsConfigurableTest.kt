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

import com.intellij.openapi.components.service
import com.intellij.testFramework.TestApplicationManager
import com.mituuz.fuzzier.entities.FuzzyMatchContainer.FilenameType.FILENAME_WITH_PATH_STYLED
import com.mituuz.fuzzier.settings.FuzzierSettingsService.RecentFilesMode.NONE
import com.mituuz.fuzzier.settings.FuzzierSettingsService.RecentFilesMode.RECENT_PROJECT_FILES
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class FuzzierSettingsConfigurableTest {
    @Suppress("unused")
    private var testApplicationManager: TestApplicationManager = TestApplicationManager.getInstance()
    private val state = service<FuzzierSettingsService>().state

    @Test
    fun `Test is modified with no changes`() {
        state.exclusionSet = setOf("Hello", "There")
        state.newTab = true
        state.recentFilesMode = NONE
        state.prioritizeShorterDirPaths = false
        state.debouncePeriod = 140
        state.filenameType = FILENAME_WITH_PATH_STYLED
        state.fileListLimit = 200
        state.fontSize = 15
        state.fileListSpacing = 2

        state.tolerance = 4
        state.multiMatch = true
        state.matchWeightPartialPath = 8
        state.matchWeightSingleChar = 6
        state.matchWeightStreakModifier = 20
        state.matchWeightFilename = 15

        val settingsConfigurable = FuzzierSettingsConfigurable()
        settingsConfigurable.createComponent()
        assertFalse(settingsConfigurable.isModified())
    }

    @Test
    fun `Test is modified with a single change`() {
        state.exclusionSet = setOf("Hello", "There")
        state.newTab = true
        state.recentFilesMode = RECENT_PROJECT_FILES
        state.debouncePeriod = 140
        state.filenameType = FILENAME_WITH_PATH_STYLED
        state.fileListLimit = 200
        state.fontSize = 15
        state.fileListSpacing = 2

        state.tolerance = 4
        state.multiMatch = true
        state.matchWeightPartialPath = 8
        state.matchWeightSingleChar = 6
        state.matchWeightStreakModifier = 20
        state.matchWeightFilename = 15

        val settingsConfigurable = FuzzierSettingsConfigurable()
        settingsConfigurable.createComponent()
        state.fontSize = 16
        assertTrue(settingsConfigurable.isModified())
    }
}