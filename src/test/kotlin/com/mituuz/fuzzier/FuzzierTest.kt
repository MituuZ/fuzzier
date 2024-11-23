package com.mituuz.fuzzier

import com.intellij.openapi.components.service
import com.intellij.testFramework.TestApplicationManager
import com.mituuz.fuzzier.entities.FuzzyMatchContainer
import com.mituuz.fuzzier.settings.FuzzierSettingsService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import javax.swing.DefaultListModel

class FuzzierTest {
    @Suppress("unused")
    private var testApplicationManager: TestApplicationManager = TestApplicationManager.getInstance()
    val fuzzierSettingsService: FuzzierSettingsService = service<FuzzierSettingsService>()

    @Test
    fun `Add file to recently used files - Null list should default to empty`() {
        val fuzzier = Fuzzier()
        val score = FuzzyMatchContainer.FuzzyScore()
        val container = FuzzyMatchContainer(score, "", "")

        fuzzierSettingsService.state.recentlySearchedFiles = null
        fuzzier.addFileToRecentlySearchedFiles(container)
        assertNotNull(fuzzierSettingsService.state.recentlySearchedFiles)
        assertEquals(1, fuzzierSettingsService.state.recentlySearchedFiles!!.size)
    }

    @Test
    fun `Add file to recently used files - Too large list is truncated`() {
        val fileListLimit = 2
        val fuzzier = Fuzzier()
        val score = FuzzyMatchContainer.FuzzyScore()
        val container = FuzzyMatchContainer(score, "", "")

        val largeList: DefaultListModel<FuzzyMatchContainer> = DefaultListModel()
        for (i in 0..25) {
            largeList.addElement(FuzzyMatchContainer(score, "" + i, "" + i))
        }

        fuzzierSettingsService.state.fileListLimit = fileListLimit

        fuzzierSettingsService.state.recentlySearchedFiles = largeList
        fuzzier.addFileToRecentlySearchedFiles(container)
        assertEquals(fileListLimit, fuzzierSettingsService.state.recentlySearchedFiles!!.size)
    }

    @Test
    fun `Add file to recently used files - Duplicate filenames are removed`() {
        val fileListLimit = 20
        val fuzzier = Fuzzier()
        val score = FuzzyMatchContainer.FuzzyScore()
        val container = FuzzyMatchContainer(score, "", "")

        val largeList: DefaultListModel<FuzzyMatchContainer> = DefaultListModel()
        for (i in 0..25) {
            largeList.addElement(FuzzyMatchContainer(score, "", ""))
        }

        fuzzierSettingsService.state.fileListLimit = fileListLimit

        fuzzierSettingsService.state.recentlySearchedFiles = largeList
        fuzzier.addFileToRecentlySearchedFiles(container)
        assertEquals(1, fuzzierSettingsService.state.recentlySearchedFiles!!.size)
    }
}