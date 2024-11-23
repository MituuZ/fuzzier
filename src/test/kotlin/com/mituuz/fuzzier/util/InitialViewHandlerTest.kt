package com.mituuz.fuzzier.util

import com.intellij.openapi.fileEditor.impl.EditorHistoryManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.mituuz.fuzzier.settings.FuzzierSettingsService
import com.mituuz.fuzzier.settings.FuzzierSettingsService.State
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class InitialViewHandlerTest {
    private lateinit var project: Project
    private lateinit var fuzzierSettingsService: FuzzierSettingsService
    private lateinit var fuzzierUtil: FuzzierUtil
    private lateinit var initialViewHandler: InitialViewHandler
    private lateinit var state: State
    private lateinit var editorHistoryManager: EditorHistoryManager

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
}