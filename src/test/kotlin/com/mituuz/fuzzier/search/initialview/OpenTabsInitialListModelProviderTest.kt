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

import com.intellij.openapi.vfs.VirtualFile
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class OpenTabsInitialListModelProviderTest {
    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `buildInitialView with no open files`() {
        val provider = OpenTabsInitialListModelProvider(emptyMap(), emptyArray())
        val model = provider.invoke()
        assertEquals(0, model.size())
    }

    @Test
    fun `buildInitialView with multiple open files`() {
        val file1 = mockk<VirtualFile>()
        val file2 = mockk<VirtualFile>()

        every { file1.isDirectory } returns false
        every { file1.path } returns "/project/src/File1.kt"
        every { file1.name } returns "File1.kt"

        every { file2.isDirectory } returns false
        every { file2.path } returns "/project/src/File2.kt"
        every { file2.name } returns "File2.kt"

        val modules = mapOf("project" to "/project/")
        val provider = OpenTabsInitialListModelProvider(modules, arrayOf(file1, file2))

        val model = provider.invoke()

        assertEquals(2, model.size())
        // Should be in reverse order of openFiles
        assertEquals("File2.kt", model.get(0).filename)
        assertEquals("src/File2.kt", model.get(0).filePath)
        assertEquals("File1.kt", model.get(1).filename)
        assertEquals("src/File1.kt", model.get(1).filePath)
    }

    @Test
    fun `buildInitialView excludes directories`() {
        val file1 = mockk<VirtualFile>()
        val dir1 = mockk<VirtualFile>()

        every { file1.isDirectory } returns false
        every { file1.path } returns "/project/src/File1.kt"
        every { file1.name } returns "File1.kt"

        every { dir1.isDirectory } returns true
        every { dir1.path } returns "/project/src/dir"
        every { dir1.name } returns "dir"

        val modules = mapOf("project" to "/project/")
        val provider = OpenTabsInitialListModelProvider(modules, arrayOf(file1, dir1))

        val model = provider.invoke()

        assertEquals(1, model.size())
        assertEquals("File1.kt", model.get(0).filename)
    }

    @Test
    fun `buildInitialView excludes files without module path`() {
        val file1 = mockk<VirtualFile>()
        val fileOutside = mockk<VirtualFile>()

        every { file1.isDirectory } returns false
        every { file1.path } returns "/project/src/File1.kt"
        every { file1.name } returns "File1.kt"

        every { fileOutside.isDirectory } returns false
        every { fileOutside.path } returns "/outside/File.kt"
        every { fileOutside.name } returns "File.kt"

        val modules = mapOf("project" to "/project/")
        val provider = OpenTabsInitialListModelProvider(modules, arrayOf(file1, fileOutside))

        val model = provider.invoke()

        assertEquals(1, model.size())
        assertEquals("File1.kt", model.get(0).filename)
    }
}
