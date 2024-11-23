package com.mituuz.fuzzier.util

import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.impl.EditorHistoryManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.TestApplicationManager
import com.mituuz.fuzzier.entities.FuzzyMatchContainer
import com.mituuz.fuzzier.settings.FuzzierSettingsService
import com.mituuz.fuzzier.settings.FuzzierSettingsService.State
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import javax.swing.DefaultListModel

class InitialViewHandlerTest {
    private lateinit var project: Project
    private lateinit var fuzzierSettingsService: FuzzierSettingsService
    private lateinit var fuzzierUtil: FuzzierUtil
    private lateinit var initialViewHandler: InitialViewHandler
    private lateinit var state: State
    private lateinit var editorHistoryManager: EditorHistoryManager
    @Suppress("unused") // Required for add to recently used files (fuzzierSettingsServiceInstance)
    private var testApplicationManager: TestApplicationManager = TestApplicationManager.getInstance()

    @BeforeEach
    fun setUp() {
        project = mock(Project::class.java)
        fuzzierSettingsService = mock(FuzzierSettingsService::class.java)
        state = mock(State::class.java)
        fuzzierUtil = mock(FuzzierUtil::class.java)
        initialViewHandler = InitialViewHandler()
        editorHistoryManager = mock(EditorHistoryManager::class.java)
        `when`(fuzzierSettingsService.state).thenReturn(state)
    }

    @Test
    fun `Recent project files - Verify that list is truncated when it goes over the file limit`() {
        val virtualFile1 = mock(VirtualFile::class.java)
        val virtualFile2 = mock(VirtualFile::class.java)
        val fileList = listOf(
            virtualFile1,
            virtualFile2
        )
        `when`(editorHistoryManager.fileList).thenReturn(fileList)
        `when`(fuzzierSettingsService.state.fileListLimit).thenReturn(1)
        `when`(virtualFile1.path).thenReturn("path")
        `when`(virtualFile1.name).thenReturn("filename1")
        `when`(virtualFile2.path).thenReturn("path")
        `when`(virtualFile2.name).thenReturn("filename2")
        `when`(fuzzierUtil.extractModulePath(anyString())).thenReturn(Pair("path", "module"))

        val result = InitialViewHandler.getRecentProjectFiles(fuzzierSettingsService, fuzzierUtil, editorHistoryManager)

        assertEquals(1, result.size())
    }

    @Test
    fun `Recent project files - Skip files that do not belong to the project`() {
        val virtualFile1 = mock(VirtualFile::class.java)
        val virtualFile2 = mock(VirtualFile::class.java)
        val fileList = listOf(
            virtualFile1,
            virtualFile2
        )
        `when`(editorHistoryManager.fileList).thenReturn(fileList)
        `when`(fuzzierSettingsService.state.fileListLimit).thenReturn(2)
        `when`(virtualFile1.path).thenReturn("path")
        `when`(virtualFile1.name).thenReturn("filename1")
        `when`(virtualFile2.path).thenReturn("path")
        `when`(virtualFile2.name).thenReturn("filename2")
        `when`(fuzzierUtil.extractModulePath(anyString())).thenReturn(Pair("path", "module"), Pair("", ""))

        val result = InitialViewHandler.getRecentProjectFiles(fuzzierSettingsService, fuzzierUtil, editorHistoryManager)

        assertEquals(1, result.size())
    }

    @Test
    fun `Recent project files - Empty list when no history`() {
        `when`(editorHistoryManager.fileList).thenReturn(emptyList())
        `when`(fuzzierSettingsService.state.fileListLimit).thenReturn(2)

        val result = InitialViewHandler.getRecentProjectFiles(fuzzierSettingsService, fuzzierUtil, editorHistoryManager)

        assertEquals(0, result.size())
    }

    @Test
    fun `Recently searched files - Null returns an empty list`() {
        `when`(fuzzierSettingsService.state.recentlySearchedFiles).thenReturn(null)
        val result = InitialViewHandler.getRecentlySearchedFiles(fuzzierSettingsService)
        assertEquals(0, result.size())
    }

    @Test
    fun `Recently searched files - Order of multiple files`() {
        val fuzzyMatchContainer1 = mock(FuzzyMatchContainer::class.java)
        val fuzzyMatchContainer2 = mock(FuzzyMatchContainer::class.java)
        val listModel = DefaultListModel<FuzzyMatchContainer>()
        listModel.addElement(fuzzyMatchContainer1)
        listModel.addElement(fuzzyMatchContainer2)
        `when`(fuzzierSettingsService.state.recentlySearchedFiles).thenReturn(listModel)

        val result = InitialViewHandler.getRecentlySearchedFiles(fuzzierSettingsService)

        assertEquals(fuzzyMatchContainer2, result[0])
        assertEquals(fuzzyMatchContainer1, result[1])
    }

    @Test
    fun `Recently searched files - Remove null elements from the list`() {
        val fuzzyMatchContainer = mock(FuzzyMatchContainer::class.java)
        val listModel = DefaultListModel<FuzzyMatchContainer>()
        listModel.addElement(fuzzyMatchContainer)
        listModel.addElement(null)
        listModel.addElement(null)
        `when`(fuzzierSettingsService.state.recentlySearchedFiles).thenReturn(listModel)

        val result = InitialViewHandler.getRecentlySearchedFiles(fuzzierSettingsService)

        assertEquals(1, result.size)
    }

    @Test
    fun `Add file to recently used files - Null list should default to empty`() {
        val fuzzierSettingsServiceInstance: FuzzierSettingsService = service<FuzzierSettingsService>()
        val score = FuzzyMatchContainer.FuzzyScore()
        val container = FuzzyMatchContainer(score, "", "")

        fuzzierSettingsServiceInstance.state.recentlySearchedFiles = null
        InitialViewHandler.addFileToRecentlySearchedFiles(container, fuzzierSettingsServiceInstance)
        assertNotNull(fuzzierSettingsServiceInstance.state.recentlySearchedFiles)
        assertEquals(1, fuzzierSettingsServiceInstance.state.recentlySearchedFiles!!.size)
    }

    @Test
    fun `Add file to recently used files - Too large list is truncated`() {
        val fuzzierSettingsServiceInstance: FuzzierSettingsService = service<FuzzierSettingsService>()
        val fileListLimit = 2
        val score = FuzzyMatchContainer.FuzzyScore()
        val container = FuzzyMatchContainer(score, "", "")

        val largeList: DefaultListModel<FuzzyMatchContainer> = DefaultListModel()
        for (i in 0..25) {
            largeList.addElement(FuzzyMatchContainer(score, "" + i, "" + i))
        }

        fuzzierSettingsServiceInstance.state.fileListLimit = fileListLimit

        fuzzierSettingsServiceInstance.state.recentlySearchedFiles = largeList
        InitialViewHandler.addFileToRecentlySearchedFiles(container, fuzzierSettingsServiceInstance)
        assertEquals(fileListLimit, fuzzierSettingsServiceInstance.state.recentlySearchedFiles!!.size)
    }

    @Test
    fun `Add file to recently used files - Duplicate filenames are removed`() {
        val fuzzierSettingsServiceInstance: FuzzierSettingsService = service<FuzzierSettingsService>()
        val fileListLimit = 20
        val score = FuzzyMatchContainer.FuzzyScore()
        val container = FuzzyMatchContainer(score, "", "")

        val largeList: DefaultListModel<FuzzyMatchContainer> = DefaultListModel()
        for (i in 0..25) {
            largeList.addElement(FuzzyMatchContainer(score, "", ""))
        }

        fuzzierSettingsServiceInstance.state.fileListLimit = fileListLimit

        fuzzierSettingsServiceInstance.state.recentlySearchedFiles = largeList
        InitialViewHandler.addFileToRecentlySearchedFiles(container, fuzzierSettingsServiceInstance)
        assertEquals(1, fuzzierSettingsServiceInstance.state.recentlySearchedFiles!!.size)
    }
}