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

import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.TestApplicationManager
import com.mituuz.fuzzier.components.FuzzyFinderComponent
import com.mituuz.fuzzier.entities.CaseMode
import com.mituuz.fuzzier.entities.GrepConfig
import com.mituuz.fuzzier.grep.backend.BackendStrategy
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FuzzyGrepTest {
    private lateinit var fGrep: FuzzyGrep

    private data class ValidVfContext(
        val file: VirtualFile,
        val clm: ChangeListManager
    )

    @BeforeEach
    fun setUp() {
        TestApplicationManager.getInstance()
        fGrep = FuzzyGrep()
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    private fun createValidVfContext(
        isDirectory: Boolean = false,
        isBinary: Boolean = false,
        isIgnored: Boolean = false,
        extension: String? = null
    ): ValidVfContext {
        val file = mockk<VirtualFile>()
        val clm = mockk<ChangeListManager>()

        every { file.isDirectory } returns isDirectory
        every { file.fileType.isBinary } returns isBinary
        every { clm.isIgnoredFile(file) } returns isIgnored
        if (extension != null) {
            every { file.extension } returns extension
        }

        return ValidVfContext(file, clm)
    }

    @Test
    fun `Directories should not be valid`() {
        val (file1, clm) = createValidVfContext(isDirectory = true)

        val res = fGrep.validVf(file1, null, clm)
        assert(!res)
    }

    @Test
    fun `Binary files should not be valid`() {
        val (file1, clm) = createValidVfContext(isBinary = true)

        val res = fGrep.validVf(file1, null, clm)
        assert(!res)
    }

    @Test
    fun `Ignored files should not be valid`() {
        val (file1, clm) = createValidVfContext(isIgnored = true)

        val res = fGrep.validVf(file1, null, clm)
        assert(!res)
    }

    @Test
    fun `null secondary field should be valid`() {
        val (file1, clm) = createValidVfContext()

        val res = fGrep.validVf(file1, null, clm)
        assert(res)
    }

    @Test
    fun `Matching secondary field should be valid`() {
        val (file1, clm) = createValidVfContext(extension = "kt")

        val res = fGrep.validVf(file1, "kt", clm)
        assert(res)
    }

    @Test
    fun `Non-matching secondary field should not be valid`() {
        val (file1, clm) = createValidVfContext(extension = "java")

        val res = fGrep.validVf(file1, "kt", clm)
        assert(!res)
    }

    @Test
    fun `findInFiles test frame should setup required mocks`() {
        val project = mockk<Project>()
        val component = mockk<FuzzyFinderComponent>()
        val backend = mockk<BackendStrategy>()
        val changelistManager = mockk<ChangeListManager>()

        mockkStatic(ChangeListManager::class)
        every { project.basePath } returns "/tmp/project"
        every { component.getSecondaryText() } returns "kt"
        every { ChangeListManager.getInstance(project) } returns changelistManager

        fGrep.component = component
        fGrep.updateBackend(backend)
        fGrep.updateGrepConfig(GrepConfig(targets = null, caseMode = CaseMode.SENSITIVE, title = "Fuzzy Grep"))

        assertEquals("/tmp/project", project.basePath)
        assertEquals("kt", component.getSecondaryText())
        assertNotNull(ChangeListManager.getInstance(project))
    }
}