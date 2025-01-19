package com.mituuz.fuzzier.entities

import com.mituuz.fuzzier.entities.FuzzyContainer.FilenameType
import com.mituuz.fuzzier.settings.FuzzierGlobalSettingsService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class OrderedContainerTest {
    @Test
    fun testGetDisplayString() {
        val orderedContainer = OrderedContainer("filePath", "basePath", "filename")
        val state = FuzzierGlobalSettingsService.State()

        state.filenameType = FilenameType.FILENAME_ONLY
        assertEquals("filename", orderedContainer.getDisplayString(state))

        state.filenameType = FilenameType.FILE_PATH_ONLY
        assertEquals("filePath", orderedContainer.getDisplayString(state))

        state.filenameType = FilenameType.FILENAME_WITH_PATH
        assertEquals("filename   (filePath)", orderedContainer.getDisplayString(state))

        state.filenameType = FilenameType.FILENAME_WITH_PATH_STYLED
        assertEquals("<html><strong>filename</strong>  <i>(filePath)</i></html>", orderedContainer.getDisplayString(state))
    }
}