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

import com.intellij.openapi.fileEditor.impl.EditorHistoryManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.TestApplicationManager
import com.mituuz.fuzzier.entities.FuzzyMatchContainer
import com.mituuz.fuzzier.entities.FuzzyMatchContainer.SerializedMatchContainer.Companion.fromFuzzyMatchContainer
import com.mituuz.fuzzier.search.initialview.DefaultInitialListModelProvider
import com.mituuz.fuzzier.settings.FuzzierGlobalSettingsService
import com.mituuz.fuzzier.settings.FuzzierSettingsService
import com.mituuz.fuzzier.settings.FuzzierSettingsService.State
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DefaultInitialListModelProviderTest {
    private lateinit var project: Project
    private lateinit var fuzzierSettingsService: FuzzierSettingsService
    private lateinit var fuzzierGlobalSettingsService: FuzzierGlobalSettingsService
    private lateinit var defaultInitialListModelProvider: DefaultInitialListModelProvider
    private lateinit var editorHistoryManager: EditorHistoryManager

    @Suppress("unused") // Required for add to recently used files (fuzzierSettingsServiceInstance)
    private var testApplicationManager: TestApplicationManager = TestApplicationManager.getInstance()

    @BeforeEach
    fun setUp() {
        project = mockk()
        fuzzierSettingsService = mockk()
        fuzzierGlobalSettingsService = mockk()
        val globalState = FuzzierGlobalSettingsService.State()
        globalState.recentFilesMode = FuzzierGlobalSettingsService.RecentFilesMode.RECENT_PROJECT_FILES
        defaultInitialListModelProvider = DefaultInitialListModelProvider(project, globalState, State())
        editorHistoryManager = mockk()
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `Recent project files - Verify that list is truncated when it goes over the file limit`() {
        val virtualFile1 = mockk<VirtualFile>()
        val virtualFile2 = mockk<VirtualFile>()
        val fileList = listOf(
            virtualFile1,
            virtualFile2
        )
        mockkStatic(EditorHistoryManager::class)
        every { EditorHistoryManager.getInstance(project) } returns editorHistoryManager
        every { editorHistoryManager.fileList } returns fileList
        val fgss = defaultInitialListModelProvider.globalState
        fgss.fileListLimit = 1
        every { virtualFile1.path } returns "/project/path/file1"
        every { virtualFile1.name } returns "filename1"
        every { virtualFile2.path } returns "/project/path/file2"
        every { virtualFile2.name } returns "filename2"

        val settingsState = State()
        settingsState.modules = mapOf("module" to "/project/path/")
        defaultInitialListModelProvider = DefaultInitialListModelProvider(project, fgss, settingsState)

        val result =
            defaultInitialListModelProvider.getRecentProjectFiles(project)

        assertEquals(1, result.size())
    }

    @Test
    fun `Recent project files - Skip files that do not belong to the project`() {
        val virtualFile1 = mockk<VirtualFile>()
        val virtualFile2 = mockk<VirtualFile>()
        val fileList = listOf(
            virtualFile1,
            virtualFile2
        )
        mockkStatic(EditorHistoryManager::class)
        every { EditorHistoryManager.getInstance(project) } returns editorHistoryManager
        every { editorHistoryManager.fileList } returns fileList
        val fgss = defaultInitialListModelProvider.globalState
        fgss.fileListLimit = 2
        every { virtualFile1.path } returns "/project/path/file1"
        every { virtualFile1.name } returns "filename1"
        every { virtualFile2.path } returns "/other/path/file2"
        every { virtualFile2.name } returns "filename2"

        val settingsState = State()
        settingsState.modules = mapOf("module" to "/project/path/")
        defaultInitialListModelProvider = DefaultInitialListModelProvider(project, fgss, settingsState)

        val result =
            defaultInitialListModelProvider.getRecentProjectFiles(project)

        assertEquals(1, result.size())
    }

    @Test
    fun `Recent project files - Empty list when no history`() {
        mockkStatic(EditorHistoryManager::class)
        every { EditorHistoryManager.getInstance(project) } returns editorHistoryManager
        val fgss = defaultInitialListModelProvider.globalState
        every { editorHistoryManager.fileList } returns emptyList()
        fgss.fileListLimit = 2

        val result =
            defaultInitialListModelProvider.getRecentProjectFiles(project)

        assertEquals(0, result.size())
    }

    @Test
    fun `getRecentlySearchedFiles returns recently searched files in reverse order`() {
        val state = State()
        state.recentlySearchedFiles = listOf(
            fromFuzzyMatchContainer(
                FuzzyMatchContainer(
                    FuzzyMatchContainer.FuzzyScore(),
                    "/old.kt",
                    "old.kt",
                    "/module",
                    FuzzyMatchContainer.FileType.FILE
                )
            ),
            fromFuzzyMatchContainer(
                FuzzyMatchContainer(
                    FuzzyMatchContainer.FuzzyScore(),
                    "/new.kt",
                    "new.kt",
                    "/module",
                    FuzzyMatchContainer.FileType.FILE
                )
            ),
        )

        val model = defaultInitialListModelProvider.getRecentlySearchedFiles(state)

        assertEquals("new.kt", model.getElementAt(0).filename)
        assertEquals("old.kt", model.getElementAt(1).filename)
    }

    @Test
    fun `Recently searched files - No files`() {
        val state = State()
        state.recentlySearchedFiles = mutableListOf()
        val result = defaultInitialListModelProvider.getRecentlySearchedFiles(state)
        assertEquals(0, result.size())
    }
}