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

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.testFramework.TestApplicationManager
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.runInEdtAndWait
import com.mituuz.fuzzier.TestUtil
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class OpenTabsCollectorTest {
    @Suppress("unused")
    private var testApplicationManager: TestApplicationManager = TestApplicationManager.getInstance()
    private val testUtil = TestUtil()
    private lateinit var fixture: CodeInsightTestFixture
    private lateinit var openTabsCollector: OpenTabsCollector

    @BeforeEach
    fun setup() {
        fixture = testUtil.setUpProject(listOf("src/file1.txt", "src/file2.txt", "src/file3.txt"))
        openTabsCollector = OpenTabsCollector()
    }

    @AfterEach
    fun tearDown() {
        fixture.tearDown()
    }

    @Test
    fun `collectFiles should collect all open files`() {
        val project = fixture.project
        val fileEditorManager = FileEditorManager.getInstance(project)
        val file1 = fixture.findFileInTempDir("src/file1.txt")
        val file2 = fixture.findFileInTempDir("src/file2.txt")

        runInEdtAndWait {
            runReadAction {
                fileEditorManager.openFile(file1, true)
                fileEditorManager.openFile(file2, true)
            }
        }

        val result = openTabsCollector.collectFiles(project, { true }, { true })

        assertEquals(2, result.size)
        assertEquals("file1.txt", result[0].name)
        assertEquals("light_idea_test_case", result[0].module)
        assertEquals("file2.txt", result[1].name)
        assertEquals("light_idea_test_case", result[1].module)
    }

    @Test
    fun `collectFiles should respect fileFilter`() {
        val project = fixture.project
        val fileEditorManager = FileEditorManager.getInstance(project)
        val file1 = fixture.findFileInTempDir("src/file1.txt")
        val file2 = fixture.findFileInTempDir("src/file2.txt")

        runInEdtAndWait {
            runReadAction {
                fileEditorManager.openFile(file1, true)
                fileEditorManager.openFile(file2, true)
            }
        }

        val result = openTabsCollector.collectFiles(project, { true }, { it.name == "file1.txt" })

        assertEquals(1, result.size)
        assertEquals("file1.txt", result[0].name)
    }

    @Test
    fun `collectFiles should respect shouldContinue`() {
        val project = fixture.project
        val fileEditorManager = FileEditorManager.getInstance(project)
        val file1 = fixture.findFileInTempDir("src/file1.txt")
        val file2 = fixture.findFileInTempDir("src/file2.txt")

        runInEdtAndWait {
            runReadAction {
                fileEditorManager.openFile(file1, true)
                fileEditorManager.openFile(file2, true)
            }
        }

        var count = 0
        val result = openTabsCollector.collectFiles(project, {
            count++
            count <= 1
        }, { true })

        assertEquals(1, result.size)
    }
}