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

package com.mituuz.fuzzier.intellij.iteration

import com.intellij.openapi.roots.ContentIterator
import com.intellij.openapi.roots.FileIndex
import com.intellij.testFramework.LightVirtualFile
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger

class IntelliJIterationFileCollectorTest {
    private lateinit var collector: IntelliJIterationFileCollector

    @BeforeEach
    fun setUp() {
        collector = IntelliJIterationFileCollector()
    }

    @Test
    fun `collectFiles returns empty list when targets are empty`() {
        val result = collector.collectFiles(emptyList()) { true }
        assertTrue(result.isEmpty())
    }

    @Test
    fun `stops iterating when shouldContinue becomes false`() {
        val file1 = LightVirtualFile("a.txt")
        val file2 = LightVirtualFile("b.txt")
        val index = mockk<FileIndex>()
        val iteratorSlot = slot<ContentIterator>()

        every { index.iterateContent(capture(iteratorSlot)) } answers {
            iteratorSlot.captured.processFile(file1)
            iteratorSlot.captured.processFile(file2)
            true
        }

        val calls = AtomicInteger(0)
        val res = collector.collectFiles(
            targets = listOf(index to "mod"),
            shouldContinue = { calls.incrementAndGet() == 1 } // true for first file, false after
        )

        assertEquals(1, res.size)
        assertEquals("a.txt", res[0].file.name)
    }

    @Test
    fun `skips directories`() {
        val file1 = LightVirtualFile("a.txt")
        val dir = mockk<LightVirtualFile>()
        val file2 = LightVirtualFile("b.txt")
        val index = mockk<FileIndex>()
        val iteratorSlot = slot<ContentIterator>()

        every { dir.isDirectory } returns true
        every { index.iterateContent(capture(iteratorSlot)) } answers {
            iteratorSlot.captured.processFile(file1)
            iteratorSlot.captured.processFile(dir)
            iteratorSlot.captured.processFile(file2)
            true
        }

        val res = collector.collectFiles(
            targets = listOf(index to "mod"),
            shouldContinue = { true }
        )

        assertEquals(2, res.size)
        assertEquals("a.txt", res[0].file.name)
        assertEquals("b.txt", res[1].file.name)
    }

    @Test
    fun `collects files from multiple file indexes`() {
        val file1 = LightVirtualFile("a.txt")
        val file2 = LightVirtualFile("b.txt")
        val file3 = LightVirtualFile("c.txt")
        val file4 = LightVirtualFile("d.txt")

        val index1 = mockk<FileIndex>()
        val index2 = mockk<FileIndex>()
        val iteratorSlot1 = slot<ContentIterator>()
        val iteratorSlot2 = slot<ContentIterator>()

        every { index1.iterateContent(capture(iteratorSlot1)) } answers {
            iteratorSlot1.captured.processFile(file1)
            iteratorSlot1.captured.processFile(file2)
            true
        }

        every { index2.iterateContent(capture(iteratorSlot2)) } answers {
            iteratorSlot2.captured.processFile(file3)
            iteratorSlot2.captured.processFile(file4)
            true
        }

        val res = collector.collectFiles(
            targets = listOf(index1 to "mod1", index2 to "mod2"),
            shouldContinue = { true }
        )

        assertEquals(4, res.size)
        assertEquals("a.txt", res[0].file.name)
        assertEquals("mod1", res[0].module)
        assertEquals("b.txt", res[1].file.name)
        assertEquals("mod1", res[1].module)
        assertEquals("c.txt", res[2].file.name)
        assertEquals("mod2", res[2].module)
        assertEquals("d.txt", res[3].file.name)
        assertEquals("mod2", res[3].module)
    }
}