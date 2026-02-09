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

import com.intellij.testFramework.TestApplicationManager
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.mituuz.fuzzier.TestUtil
import com.mituuz.fuzzier.settings.FuzzierSettingsService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class IntelliJIterationFileCollectorTest {
    @Suppress("unused")
    private var testApplicationManager: TestApplicationManager = TestApplicationManager.getInstance()
    private val testUtil = TestUtil()
    private lateinit var fixture: CodeInsightTestFixture
    private lateinit var projectState: FuzzierSettingsService.State
    private lateinit var collector: IntelliJIterationFileCollector

    @BeforeEach
    fun setup() {
        projectState = FuzzierSettingsService.State()
        collector = IntelliJIterationFileCollector(projectState)
    }

    @AfterEach
    fun tearDown() {
        if (::fixture.isInitialized) {
            fixture.tearDown()
        }
    }

    @Test
    fun `collectFiles should collect all files in project mode`() {
        fixture = testUtil.setUpProject(listOf("src/file1.txt", "src/file2.txt"))
        projectState.isProject = true

        val result = collector.collectFiles(
            project = fixture.project,
            shouldContinue = { true },
            fileFilter = { !it.isDirectory }
        )

        assertEquals(2, result.size)
        assertTrue(result.any { it.name == "file1.txt" })
        assertTrue(result.any { it.name == "file2.txt" })
        assertEquals("Test", result[0].module)
    }

    @Test
    fun `collectFiles should collect files in module mode`() {
        fixture = testUtil.setUpDuoModuleProject(listOf("src1/file1.txt"), listOf("src2/file2.txt"))
        projectState.isProject = false

        val result = collector.collectFiles(
            project = fixture.project,
            shouldContinue = { true },
            fileFilter = { !it.isDirectory }
        )

        assertEquals(2, result.size)
        val file1 = result.find { it.name == "file1.txt" }
        val file2 = result.find { it.name == "file2.txt" }

        assertEquals("src1", file1?.module)
        assertEquals("src2", file2?.module)
    }

    @Test
    fun `collectFiles should respect fileFilter`() {
        fixture = testUtil.setUpProject(listOf("src/file1.txt", "src/file2.txt"))
        projectState.isProject = true

        val result = collector.collectFiles(
            project = fixture.project,
            shouldContinue = { true },
            fileFilter = { it.name == "file1.txt" }
        )

        assertEquals(1, result.size)
        assertEquals("file1.txt", result[0].name)
    }

    @Test
    fun `collectFiles should respect shouldContinue`() {
        fixture = testUtil.setUpProject(listOf("src/file1.txt", "src/file2.txt", "src/file3.txt"))
        projectState.isProject = true

        val result = collector.collectFiles(
            project = fixture.project,
            shouldContinue = { false },
            fileFilter = { !it.isDirectory }
        )

        assertTrue(result.isEmpty())
    }
}
