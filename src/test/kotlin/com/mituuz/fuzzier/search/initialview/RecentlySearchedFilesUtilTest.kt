/*
 *  MIT License
 *
 *  Copyright (c) 2025 Mitja Leino
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package com.mituuz.fuzzier.search.initialview

import com.intellij.openapi.components.service
import com.intellij.testFramework.TestApplicationManager
import com.mituuz.fuzzier.entities.FuzzyMatchContainer
import com.mituuz.fuzzier.entities.FuzzyMatchContainer.FileType.FILE
import com.mituuz.fuzzier.settings.FuzzierGlobalSettingsService
import com.mituuz.fuzzier.settings.FuzzierSettingsService
import io.mockk.unmockkAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import javax.swing.DefaultListModel

class RecentlySearchedFilesUtilTest {
    @Suppress("unused") // Required for add to recently used files (fuzzierSettingsServiceInstance)
    private var testApplicationManager: TestApplicationManager = TestApplicationManager.getInstance()

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `Add file to recently used files - Null list should default to empty`() {
        val fuzzierSettingsServiceInstance: FuzzierSettingsService = service<FuzzierSettingsService>()
        val fgss = service<FuzzierGlobalSettingsService>().state
        val score = FuzzyMatchContainer.FuzzyScore()
        val container = FuzzyMatchContainer(score, "", "", "", FILE)

        fuzzierSettingsServiceInstance.state.recentlySearchedFiles = null
        addFileToRecentlySearchedFiles(
            container,
            fuzzierSettingsServiceInstance.state,
            fgss
        )
        assertNotNull(fuzzierSettingsServiceInstance.state.getRecentlySearchedFilesAsFuzzyMatchContainer())
        assertEquals(1, fuzzierSettingsServiceInstance.state.getRecentlySearchedFilesAsFuzzyMatchContainer().size)
    }

    @Test
    fun `Add file to recently used files - Too large list is truncated`() {
        val fuzzierSettingsServiceInstance: FuzzierSettingsService = service<FuzzierSettingsService>()
        val fgss = service<FuzzierGlobalSettingsService>().state
        val fileListLimit = 2
        val score = FuzzyMatchContainer.FuzzyScore()
        val container = FuzzyMatchContainer(score, "", "", "", FILE)

        val largeList: DefaultListModel<FuzzyMatchContainer> = DefaultListModel()
        for (i in 0..25) {
            largeList.addElement(FuzzyMatchContainer(score, "" + i, "" + i, "", FILE))
        }

        fgss.fileListLimit = fileListLimit

        fuzzierSettingsServiceInstance.state.recentlySearchedFiles =
            FuzzyMatchContainer.SerializedMatchContainer.fromListModel(largeList)
        addFileToRecentlySearchedFiles(
            container,
            fuzzierSettingsServiceInstance.state,
            fgss
        )
        assertEquals(
            fileListLimit,
            fuzzierSettingsServiceInstance.state.getRecentlySearchedFilesAsFuzzyMatchContainer().size
        )
    }

    @Test
    fun `Add file to recently used files - Duplicate filenames are removed`() {
        val fuzzierSettingsServiceInstance: FuzzierSettingsService = service<FuzzierSettingsService>()
        val fgss = service<FuzzierGlobalSettingsService>().state
        val fileListLimit = 20
        val score = FuzzyMatchContainer.FuzzyScore()
        val container = FuzzyMatchContainer(score, "", "", "", FILE)

        val largeList: DefaultListModel<FuzzyMatchContainer> = DefaultListModel()
        repeat(26) {
            largeList.addElement(FuzzyMatchContainer(score, "", "", "", FILE))
        }

        fgss.fileListLimit = fileListLimit

        fuzzierSettingsServiceInstance.state.recentlySearchedFiles =
            FuzzyMatchContainer.SerializedMatchContainer.fromListModel(largeList)
        addFileToRecentlySearchedFiles(
            container,
            fuzzierSettingsServiceInstance.state,
            fgss
        )
        assertEquals(1, fuzzierSettingsServiceInstance.state.getRecentlySearchedFilesAsFuzzyMatchContainer().size)
    }
}
