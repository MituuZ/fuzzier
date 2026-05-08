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

package com.mituuz.fuzzier.grep

import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.TestApplicationManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FuzzyGrepTest {
    private lateinit var fGrep: FuzzyGrep

    @BeforeEach
    fun setUp() {
        TestApplicationManager.getInstance()
        fGrep = FuzzyGrep()
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `Directories should not be valid`() {
        val file1 = mockk<VirtualFile>()
        val clm = mockk<ChangeListManager>()

        every { file1.isDirectory } returns true

        val res = fGrep.validVf(file1, null, clm)
        assert(!res)
    }

    @Test
    fun `Binary files should not be valid`() {
        val file1 = mockk<VirtualFile>()
        val clm = mockk<ChangeListManager>()

        every { file1.isDirectory } returns false
        every { file1.fileType.isBinary } returns true

        val res = fGrep.validVf(file1, null, clm)
        assert(!res)
    }

    @Test
    fun `Ignored files should not be valid`() {
        val file1 = mockk<VirtualFile>()
        val clm = mockk<ChangeListManager>()

        every { file1.isDirectory } returns false
        every { file1.fileType.isBinary } returns false
        every { clm.isIgnoredFile(file1) } returns true

        val res = fGrep.validVf(file1, null, clm)
        assert(!res)
    }

    @Test
    fun `null secondary field should be valid`() {
        val file1 = mockk<VirtualFile>()
        val clm = mockk<ChangeListManager>()

        every { file1.isDirectory } returns false
        every { file1.fileType.isBinary } returns false
        every { clm.isIgnoredFile(file1) } returns false

        val res = fGrep.validVf(file1, null, clm)
        assert(res)
    }

    @Test
    fun `Matching secondary field should be valid`() {
        val file1 = mockk<VirtualFile>()
        val clm = mockk<ChangeListManager>()

        every { file1.isDirectory } returns false
        every { file1.fileType.isBinary } returns false
        every { clm.isIgnoredFile(file1) } returns false

        every { file1.extension } returns "kt"

        val res = fGrep.validVf(file1, "kt", clm)
        assert(res)
    }

    @Test
    fun `Non-matching secondary field should not be valid`() {
        val file1 = mockk<VirtualFile>()
        val clm = mockk<ChangeListManager>()

        every { file1.isDirectory } returns false
        every { file1.fileType.isBinary } returns false
        every { clm.isIgnoredFile(file1) } returns false

        every { file1.extension } returns "java"

        val res = fGrep.validVf(file1, "kt", clm)
        assert(!res)
    }
}