package com.mituuz.fuzzier.settings

import com.intellij.openapi.components.service
import com.intellij.testFramework.TestApplicationManager
import com.mituuz.fuzzier.entities.FuzzyMatchContainer
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
        state.debouncePeriod = 140
        state.filenameType = FuzzyMatchContainer.FilenameType.FILENAME_WITH_PATH_STYLED
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
        state.debouncePeriod = 140
        state.filenameType = FuzzyMatchContainer.FilenameType.FILENAME_WITH_PATH_STYLED
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