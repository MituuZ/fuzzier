/*
MIT License

Copyright (c) 2025 Mitja Leino

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
import com.mituuz.fuzzier.entities.FuzzyContainer.FilenameType.FILENAME_ONLY
import com.mituuz.fuzzier.entities.FuzzyContainer.FilenameType.FILENAME_WITH_PATH_STYLED
import com.mituuz.fuzzier.settings.FuzzierGlobalSettingsService.RecentFilesMode.NONE
import com.mituuz.fuzzier.settings.FuzzierGlobalSettingsService.RecentFilesMode.RECENT_PROJECT_FILES
import com.mituuz.fuzzier.settings.FuzzierGlobalSettingsService.SearchPosition.TOP
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FuzzierGlobalSettingsConfigurableTest {
    @Suppress("unused")
    private var testApplicationManager: TestApplicationManager = TestApplicationManager.getInstance()
    private val state = service<FuzzierGlobalSettingsService>().state
    private val settingsConfigurable = FuzzierGlobalSettingsConfigurable()

    @BeforeEach
    fun setUp() {
        state.newTab = true
        state.recentFilesMode = RECENT_PROJECT_FILES
        state.prioritizeShorterDirPaths = false
        state.debouncePeriod = 140
        state.resetWindow = false
        state.fileListLimit = 200

        state.filenameType = FILENAME_WITH_PATH_STYLED
        state.highlightFilename = false
        state.fileListFontSize = 15
        state.previewFontSize = 0
        state.fileListSpacing = 2

        state.tolerance = 4
        state.multiMatch = true
        state.matchWeightPartialPath = 8
        state.matchWeightSingleChar = 6
        state.matchWeightStreakModifier = 20
        state.matchWeightFilename = 15
    }

    @Test
    fun newTab() {
        pre()
        state.newTab = false
        assertTrue(settingsConfigurable.isModified)
    }

    @Test
    fun recentFilesMode() {
        pre()
        state.recentFilesMode = NONE
        assertTrue(settingsConfigurable.isModified)
    }

    @Test
    fun prioritizeShorterDirPaths() {
        pre()
        state.prioritizeShorterDirPaths = true
        assertTrue(settingsConfigurable.isModified)
    }

    @Test
    fun debouncePeriod() {
        pre()
        state.debouncePeriod = 150
        assertTrue(settingsConfigurable.isModified)
    }

    @Test
    fun fileListLimit() {
        pre()
        state.fileListLimit = 250
        assertTrue(settingsConfigurable.isModified)
    }

    @Test
    fun filenameType() {
        pre()
        state.filenameType = FILENAME_ONLY
        assertTrue(settingsConfigurable.isModified)
    }

    @Test
    fun highlightFilename() {
        pre()
        state.highlightFilename = true
        assertTrue(settingsConfigurable.isModified)
    }

    @Test
    fun fileListFontSize() {
        pre()
        state.fileListFontSize = 16
        assertTrue(settingsConfigurable.isModified)
    }

    @Test
    fun previewFontSize() {
        pre()
        state.previewFontSize = 14
        assertTrue(settingsConfigurable.isModified)
    }

    @Test
    fun fileListSpacing() {
        pre()
        state.fileListSpacing = 3
        assertTrue(settingsConfigurable.isModified)
    }

    @Test
    fun tolerance() {
        pre()
        state.tolerance = 5
        assertTrue(settingsConfigurable.isModified)
    }

    @Test
    fun multiMatch() {
        pre()
        state.multiMatch = false
        assertTrue(settingsConfigurable.isModified)
    }

    @Test
    fun matchWeightPartialPath() {
        pre()
        state.matchWeightPartialPath = 9
        assertTrue(settingsConfigurable.isModified)
    }

    @Test
    fun matchWeightSingleChar() {
        pre()
        state.matchWeightSingleChar = 7
        assertTrue(settingsConfigurable.isModified)
    }

    @Test
    fun matchWeightStreakModifier() {
        pre()
        state.matchWeightStreakModifier = 21
        assertTrue(settingsConfigurable.isModified)
    }

    @Test
    fun matchWeightFilename() {
        pre()
        state.matchWeightFilename = 16
        assertTrue(settingsConfigurable.isModified)
    }

    @Test
    fun dimensionKeyWidth() {
        pre()
        state.defaultPopupWidth = 400
        assertTrue(settingsConfigurable.isModified)
        settingsConfigurable.apply()
        assertTrue(state.resetWindow)
    }

    @Test
    fun searchPosition() {
        pre()
        state.searchPosition = TOP
        assertTrue(settingsConfigurable.isModified)
        settingsConfigurable.apply()
        assertTrue(state.resetWindow)
    }

    @Test
    fun dimensionKeyHeight() {
        pre()
        state.defaultPopupHeight = 700
        assertTrue(settingsConfigurable.isModified)
        settingsConfigurable.apply()
        assertTrue(state.resetWindow)
    }

    private fun pre() {
        settingsConfigurable.createComponent()
        assertFalse(settingsConfigurable.isModified)
        assertFalse(state.resetWindow)
    }
}