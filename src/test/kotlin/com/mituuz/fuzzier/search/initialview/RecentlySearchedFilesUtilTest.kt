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
import com.mituuz.fuzzier.entities.FileAccessData
import com.mituuz.fuzzier.entities.FuzzyMatchContainer
import com.mituuz.fuzzier.entities.FuzzyMatchContainer.FileType.FILE
import com.mituuz.fuzzier.settings.FuzzierSettingsService
import io.mockk.unmockkAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RecentlySearchedFilesUtilTest {
    @Suppress("unused") // Required for add to recently used files (fuzzierSettingsServiceInstance)
    private var testApplicationManager: TestApplicationManager = TestApplicationManager.getInstance()
    private lateinit var fuzzierSettingsServiceInstance: FuzzierSettingsService

    @BeforeEach
    fun setUp() {
        fuzzierSettingsServiceInstance = service<FuzzierSettingsService>()
        fuzzierSettingsServiceInstance.state.recentlySearchedFiles = mutableListOf()
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    private fun createContainer(path: String = ""): FuzzyMatchContainer {
        return FuzzyMatchContainer(FuzzyMatchContainer.FuzzyScore(), path, path, path, FILE)
    }

    @Test
    fun `Add file to recently used files - Null list should default to empty`() {
        val container = createContainer()

        fuzzierSettingsServiceInstance.state.recentlySearchedFiles = null
        addFileToRecentlySearchedFiles(
            container,
            fuzzierSettingsServiceInstance.state,
            20, 100
        )
        assertNotNull(fuzzierSettingsServiceInstance.state.recentlySearchedFiles)
        assertEquals(1, fuzzierSettingsServiceInstance.state.recentlySearchedFiles?.size)
    }

    @Test
    fun `Add file to recently used files - Too large list is truncated`() {
        val fileListLimit = 2
        val container = createContainer()

        val largeList: MutableList<FuzzyMatchContainer> = mutableListOf()
        for (i in 0..25) {
            largeList.add(createContainer("" + i))
        }

        fuzzierSettingsServiceInstance.state.recentlySearchedFiles =
            largeList.map { FuzzyMatchContainer.SerializedMatchContainer.fromFuzzyMatchContainer(it) }
        addFileToRecentlySearchedFiles(
            container,
            fuzzierSettingsServiceInstance.state,
            fileListLimit, 100
        )
        assertEquals(
            fileListLimit,
            fuzzierSettingsServiceInstance.state.recentlySearchedFiles?.size
        )
    }

    @Test
    fun `Add file to recently used files - Duplicate filenames are removed`() {
        val container = createContainer()

        val largeList: MutableList<FuzzyMatchContainer> = mutableListOf()
        repeat(26) {
            largeList.add(createContainer())
        }

        fuzzierSettingsServiceInstance.state.recentlySearchedFiles =
            largeList.map { FuzzyMatchContainer.SerializedMatchContainer.fromFuzzyMatchContainer(it) }
        addFileToRecentlySearchedFiles(
            container,
            fuzzierSettingsServiceInstance.state,
            20, 100
        )
        assertEquals(1, fuzzierSettingsServiceInstance.state.recentlySearchedFiles?.size)
    }

    @Test
    fun `addFileToLRUCache - Add new file to empty cache`() {
        val recentFiles = mutableListOf<FileAccessData>()
        val container = createContainer("path1")
        val result = addFileToLRUCache(container, recentFiles, 5)

        assertEquals(1, result.size)
        assertEquals("path1", result[0].filePath)
        assertEquals(1, result[0].accessCount)
    }

    @Test
    fun `addFileToLRUCache - Add new file to non-empty cache`() {
        val recentFiles = mutableListOf(
            FileAccessData("path1", 1)
        )
        val container = createContainer("path2")
        val result = addFileToLRUCache(container, recentFiles, 5)

        assertEquals(2, result.size)
        assertEquals("path2", result[0].filePath)
        assertEquals(1, result[0].accessCount)
        assertEquals("path1", result[1].filePath)
    }

    @Test
    fun `addFileToLRUCache - Add existing file`() {
        val recentFiles = mutableListOf(
            FileAccessData("path1", 1),
            FileAccessData("path2", 1)
        )
        val container = createContainer("path2")
        val result = addFileToLRUCache(container, recentFiles, 5)

        assertEquals(2, result.size)
        assertEquals("path2", result[0].filePath)
        assertEquals(2, result[0].accessCount)
        assertEquals("path1", result[1].filePath)
    }

    @Test
    fun `addFileToLRUCache - Exceeding max size`() {
        val recentFiles = mutableListOf(
            FileAccessData("path2", 1),
            FileAccessData("path1", 1)
        )
        val container = createContainer("path3")
        val result = addFileToLRUCache(container, recentFiles, 2)

        assertEquals(2, result.size)
        assertEquals("path3", result[0].filePath)
        assertEquals("path2", result[1].filePath)
    }

    @Test
    fun `addFileToLRUCache - Max size 0`() {
        val recentFiles = mutableListOf<FileAccessData>()
        val container = createContainer("path1")
        val result = addFileToLRUCache(container, recentFiles, 0)

        assertEquals(0, result.size)
    }
}
