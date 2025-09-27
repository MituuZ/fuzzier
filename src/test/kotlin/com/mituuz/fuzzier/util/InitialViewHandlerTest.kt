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
package com.mituuz.fuzzier.util

import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.impl.EditorHistoryManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.TestApplicationManager
import com.mituuz.fuzzier.entities.FuzzyMatchContainer
import com.mituuz.fuzzier.settings.FuzzierGlobalSettingsService
import com.mituuz.fuzzier.settings.FuzzierSettingsService
import com.mituuz.fuzzier.settings.FuzzierSettingsService.State
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import javax.swing.DefaultListModel

class InitialViewHandlerTest {
    private lateinit var project: Project
    private lateinit var fuzzierSettingsService: FuzzierSettingsService
    private lateinit var fuzzierGlobalSettingsService: FuzzierGlobalSettingsService
    private lateinit var fuzzierUtil: FuzzierUtil
    private lateinit var initialViewHandler: InitialViewHandler
    private lateinit var state: State
    private lateinit var editorHistoryManager: EditorHistoryManager

    @Suppress("unused") // Required for add to recently used files (fuzzierSettingsServiceInstance)
    private var testApplicationManager: TestApplicationManager = TestApplicationManager.getInstance()

    @BeforeEach
    fun setUp() {
        project = mockk()
        fuzzierSettingsService = mockk()
        fuzzierGlobalSettingsService = mockk()
        state = mockk()
        fuzzierUtil = mockk()
        initialViewHandler = InitialViewHandler()
        editorHistoryManager = mockk()
        every { fuzzierSettingsService.state } returns state
    }

    @Test
    @Disabled
    fun `Recent project files - Verify that list is truncated when it goes over the file limit`() {
        val virtualFile1 = mockk<VirtualFile>()
        val virtualFile2 = mockk<VirtualFile>()
        val fileList = listOf(
            virtualFile1,
            virtualFile2
        )
        val fgss = service<FuzzierGlobalSettingsService>().state
        every { editorHistoryManager.fileList } returns fileList
        fgss.fileListLimit = 1
        every { virtualFile1.path } returns "path"
        every { virtualFile1.name } returns "filename1"
        every { virtualFile2.path } returns "path"
        every { virtualFile2.name } returns "filename2"
        every { fuzzierUtil.extractModulePath(any(), project) } returns Pair("path", "module")
        val result = InitialViewHandler.getRecentProjectFiles(fgss, fuzzierUtil, editorHistoryManager, project)

        assertEquals(1, result.size())
    }

    @Test
    @Disabled
    fun `Recent project files - Skip files that do not belong to the project`() {
        val virtualFile1 = mockk<VirtualFile>()
        val virtualFile2 = mockk<VirtualFile>()
        val fileList = listOf(
            virtualFile1,
            virtualFile2
        )
        val fgss = service<FuzzierGlobalSettingsService>().state
        every { editorHistoryManager.fileList } returns fileList
        fgss.fileListLimit = 2
        every { virtualFile1.path } returns "path"
        every { virtualFile1.name } returns "filename1"
        every { virtualFile2.path } returns "path"
        every { virtualFile2.name } returns "filename2"
        every { fuzzierUtil.extractModulePath(any(), project) } returns Pair("path", "module") andThen Pair("", "")
        val result = InitialViewHandler.getRecentProjectFiles(fgss, fuzzierUtil, editorHistoryManager, project)

        assertEquals(1, result.size())
    }

    @Test
    fun `Recent project files - Empty list when no history`() {
        val fgss = service<FuzzierGlobalSettingsService>().state
        every { editorHistoryManager.fileList } returns emptyList()
        fgss.fileListLimit = 2

        val result = InitialViewHandler.getRecentProjectFiles(fgss, fuzzierUtil, editorHistoryManager, project)

        assertEquals(0, result.size())
    }

    @Test
    fun `Recently searched files - Order of multiple files`() {
        val fuzzyMatchContainer1 = mockk<FuzzyMatchContainer>()
        val fuzzyMatchContainer2 = mockk<FuzzyMatchContainer>()
        val listModel = DefaultListModel<FuzzyMatchContainer>()
        listModel.addElement(fuzzyMatchContainer1)
        listModel.addElement(fuzzyMatchContainer2)
        every { fuzzierSettingsService.state.getRecentlySearchedFilesAsFuzzyMatchContainer() } returns listModel

        val result = InitialViewHandler.getRecentlySearchedFiles(fuzzierSettingsService.state)

        assertEquals(fuzzyMatchContainer2, result[0])
        assertEquals(fuzzyMatchContainer1, result[1])
    }

    @Test
    fun `Recently searched files - Remove null elements from the list`() {
        val fuzzyMatchContainer = mockk<FuzzyMatchContainer>()
        val listModel = DefaultListModel<FuzzyMatchContainer>()
        listModel.addElement(fuzzyMatchContainer)
        listModel.addElement(null)
        listModel.addElement(null)
        every { fuzzierSettingsService.state.getRecentlySearchedFilesAsFuzzyMatchContainer() } returns listModel

        val result = InitialViewHandler.getRecentlySearchedFiles(fuzzierSettingsService.state)

        assertEquals(1, result.size)
    }

    @Test
    fun `Add file to recently used files - Null list should default to empty`() {
        val fuzzierSettingsServiceInstance: FuzzierSettingsService = service<FuzzierSettingsService>()
        val fgss = service<FuzzierGlobalSettingsService>().state
        val score = FuzzyMatchContainer.FuzzyScore()
        val container = FuzzyMatchContainer(score, "", "", "")

        fuzzierSettingsServiceInstance.state.recentlySearchedFiles = null
        InitialViewHandler.addFileToRecentlySearchedFiles(container, fuzzierSettingsServiceInstance.state, fgss)
        assertNotNull(fuzzierSettingsServiceInstance.state.getRecentlySearchedFilesAsFuzzyMatchContainer())
        assertEquals(1, fuzzierSettingsServiceInstance.state.getRecentlySearchedFilesAsFuzzyMatchContainer().size)
    }

    @Test
    fun `Add file to recently used files - Too large list is truncated`() {
        val fuzzierSettingsServiceInstance: FuzzierSettingsService = service<FuzzierSettingsService>()
        val fgss = service<FuzzierGlobalSettingsService>().state
        val fileListLimit = 2
        val score = FuzzyMatchContainer.FuzzyScore()
        val container = FuzzyMatchContainer(score, "", "", "")

        val largeList: DefaultListModel<FuzzyMatchContainer> = DefaultListModel()
        for (i in 0..25) {
            largeList.addElement(FuzzyMatchContainer(score, "" + i, "" + i, ""))
        }

        fgss.fileListLimit = fileListLimit

        fuzzierSettingsServiceInstance.state.recentlySearchedFiles =
            FuzzyMatchContainer.SerializedMatchContainer.fromListModel(largeList)
        InitialViewHandler.addFileToRecentlySearchedFiles(container, fuzzierSettingsServiceInstance.state, fgss)
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
        val container = FuzzyMatchContainer(score, "", "", "")

        val largeList: DefaultListModel<FuzzyMatchContainer> = DefaultListModel()
        repeat(26) {
            largeList.addElement(FuzzyMatchContainer(score, "", "", ""))
        }

        fgss.fileListLimit = fileListLimit

        fuzzierSettingsServiceInstance.state.recentlySearchedFiles =
            FuzzyMatchContainer.SerializedMatchContainer.fromListModel(largeList)
        InitialViewHandler.addFileToRecentlySearchedFiles(container, fuzzierSettingsServiceInstance.state, fgss)
        assertEquals(1, fuzzierSettingsServiceInstance.state.getRecentlySearchedFilesAsFuzzyMatchContainer().size)
    }
}