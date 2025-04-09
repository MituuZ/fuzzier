/*
MIT License

Copyright (c) 2025 Mitja Leino

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/
package com.mituuz.fuzzier.util

import com.intellij.openapi.components.service
import com.intellij.testFramework.TestApplicationManager
import com.mituuz.fuzzier.TestUtil
import com.mituuz.fuzzier.entities.FuzzyContainer
import com.mituuz.fuzzier.entities.FuzzyMatchContainer
import com.mituuz.fuzzier.entities.FuzzyMatchContainer.FuzzyScore
import com.mituuz.fuzzier.settings.FuzzierSettingsService
import kotlinx.collections.immutable.persistentMapOf
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import javax.swing.DefaultListModel

class FuzzierUtilTest {
    @Suppress("unused")
    private val testApplicationManager = TestApplicationManager.getInstance()
    private val fuzzierUtil = FuzzierUtil()
    private val listModel = DefaultListModel<FuzzyContainer>()
    private lateinit var result: DefaultListModel<FuzzyContainer>
    private val testUtil = TestUtil()

    @BeforeEach
    fun setUp() {
        listModel.clear()
    }

    @Test
    fun `Parse modules multiple modules and roots`() {
        val myFixture = testUtil.setUpMultiModuleProject(listOf("src1", "/src1/file1"), listOf("src2", "/src2/file2"), listOf("src3", "/src3/file3"))
        fuzzierUtil.parseModules(myFixture.project)

        val modules = myFixture.project.service<FuzzierSettingsService>().state.modules
        assertEquals(3, modules.size)

        // The base path should not include the module dir itself, because it is shown in the file path
        assertEquals(modules["src1"], modules["src2"])
        assertEquals(modules["src1"], modules["src3"])
    }

    @Test
    fun `Parse modules multiple modules and unique roots`() {
        val myFixture = testUtil.setUpMultiModuleProject(listOf("path/src1", "/path/src1/file1"), listOf("to/src2", "/to/src2/file2"), listOf("module/src3", "/module/src3/file3"))
        fuzzierUtil.parseModules(myFixture.project)

        val modules = myFixture.project.service<FuzzierSettingsService>().state.modules
        assertEquals(3, modules.size)

        // When each module is in a separate path, the root should contain up to the module directory
        assertNotEquals(modules["src1"], modules["src2"])
        assertNotEquals(modules["src1"], modules["src3"])
        assertNotEquals(modules["src2"], modules["src3"])
    }

    @Test
    fun `Parse modules multiple modules with single root`() {
        val myFixture = testUtil.setUpMultiModuleProject(listOf("src1", "/src1/file1"), listOf("src1/module1", "/src1/module1/file1"), listOf("src1/module2", "/src1/module2/file1"))
        fuzzierUtil.parseModules(myFixture.project)

        val modules = myFixture.project.service<FuzzierSettingsService>().state.modules

        // All submodules should share the same base path with the root module
        assertEquals(3, modules.size)
        assertEquals(modules["src1"], modules["module1"])
        assertEquals(modules["src1"], modules["module2"])
    }

    @Test
    fun `Remove module paths, mixed set of modules`() {
        val myFixture = testUtil.setUpMultiModuleProject(listOf("src1", "/src1/file1"),
            listOf("src1/module1", "/src1/module1/file1"), listOf("src2", "/src2/file1"))
        val project = myFixture.project
        fuzzierUtil.parseModules(project)

        val modules = myFixture.project.service<FuzzierSettingsService>().state.modules
        assertEquals(3, modules.size)

        var file = myFixture.findFileInTempDir("/src1/file1")
        assertEquals("/src1/file1", fuzzierUtil.extractModulePath(file.path, project).first)
        var finalPath = fuzzierUtil.extractModulePath(file.path, project).second.substringAfterLast("/")
        assertTrue(finalPath.startsWith("unitTest"))

        file = myFixture.findFileInTempDir("/src1/module1/file1")
        assertEquals("/src1/module1/file1", fuzzierUtil.extractModulePath(file.path, project).first)
        finalPath = fuzzierUtil.extractModulePath(file.path, project).second.substringAfterLast("/")
        assertTrue(finalPath.startsWith("unitTest"))

        file = myFixture.findFileInTempDir("/src2/file1")
        assertEquals("/src2/file1", fuzzierUtil.extractModulePath(file.path, project).first)
        finalPath = fuzzierUtil.extractModulePath(file.path, project).second.substringAfterLast("/")
        assertTrue(finalPath.startsWith("unitTest"))
    }

    @Test
    fun `Remove module paths, include module dir on multi module project`() {
        val myFixture = testUtil.setUpMultiModuleProject(listOf("path/src1", "/path/src1/file1"), listOf("to/src2", "/to/src2/file2"), listOf("module/src3", "/module/src3/file3"))
        val project = myFixture.project
        fuzzierUtil.parseModules(project)

        val modules = myFixture.project.service<FuzzierSettingsService>().state.modules
        assertEquals(3, modules.size)

        var file = myFixture.findFileInTempDir("/path/src1/file1")
        assertEquals("/src1/file1", fuzzierUtil.extractModulePath(file.path, project).first)
        var finalPath = fuzzierUtil.extractModulePath(file.path, project).second.substringAfterLast("/")
        assertTrue(finalPath.startsWith("path"))

        file = myFixture.findFileInTempDir("/to/src2/file2")
        assertEquals("/src2/file2", fuzzierUtil.extractModulePath(file.path, project).first)
        finalPath = fuzzierUtil.extractModulePath(file.path, project).second.substringAfterLast("/")
        assertTrue(finalPath.startsWith("to"))

        file = myFixture.findFileInTempDir("/module/src3/file3")
        assertEquals("/src3/file3", fuzzierUtil.extractModulePath(file.path, project).first)
        finalPath = fuzzierUtil.extractModulePath(file.path, project).second.substringAfterLast("/")
        assertTrue(finalPath.startsWith("module"))
    }

    @Test
    fun `Remove module paths, point only to project root`() {
        val myFixture = testUtil.setUpMultiModuleProject(listOf("path/src1", "/path/src1/file1"))
        fuzzierUtil.parseModules(myFixture.project)

        val modules = myFixture.project.service<FuzzierSettingsService>().state.modules
        assertEquals(1, modules.size)

        val file = myFixture.findFileInTempDir("/path/src1/file1")
        assertEquals("/file1", fuzzierUtil.extractModulePath(file.path, myFixture.project).first)
    }

    @Test
    fun `Remove module paths, file not included`() {
        val myFixture = testUtil.setUpMultiModuleProject(listOf("path/src1", "/path/src1/file1"))
        fuzzierUtil.parseModules(myFixture.project)

        val modules = myFixture.project.service<FuzzierSettingsService>().state.modules
        assertEquals(1, modules.size)

        assertEquals(Pair("/no/such/file", ""), fuzzierUtil.extractModulePath("/no/such/file", myFixture.project))
    }

    @Test
    fun parseModulesSingleModule() {
        val myFixture = testUtil.setUpMultiModuleProject(listOf("src1", "/src1/file1"))
        fuzzierUtil.parseModules(myFixture.project)

        val modules = myFixture.project.service<FuzzierSettingsService>().state.modules
        assertEquals(1, modules.size)
    }

    @Test
    fun `Sort and limit under limit`() {
        addElement(1, "file1")
        addElement(2, "file2")
        addElement(3, "file3")

        runWithLimit(5)
        assertEquals(3, result.size)
        assertEquals("file3", result[0].filename)
        assertEquals("file2", result[1].filename)
        assertEquals("file1", result[2].filename)
    }

    @Test
    fun `Sort and limit equal to limit`() {
        addElement(1, "file1")
        addElement(8, "file2")
        addElement(3, "file3")

        runWithLimit(3)
        assertEquals(3, result.size)
        assertEquals("file2", result[0].filename)
        assertEquals("file3", result[1].filename)
        assertEquals("file1", result[2].filename)
    }

    @Test
    fun `Sort and limit over limit`() {
        addElement(1, "file1")
        addElement(8, "file2")
        addElement(3, "file3")
        addElement(4, "file4")

        runWithLimit(2)
        assertEquals(2, result.size)
        assertEquals("file2", result[0].filename)
        assertEquals("file4", result[1].filename)
    }

    @Test
    fun `Empty list`() {
        runWithLimit(2)
        assertEquals(0, result.size)
    }

    @Test
    fun `Prioritize file paths with same score`() {
        addElement(0, "file1", "1")
        addElement(0, "file2", "123")
        addElement(0, "file3", "12")
        addElement(0, "file4", "1234")

        runPrioritizedList()
        assertEquals(4, result.size)
        assertEquals("file1", result[0].filename)
        assertEquals("file3", result[1].filename)
        assertEquals("file2", result[2].filename)
        assertEquals("file4", result[3].filename)
    }

    @Test
    fun `Prioritize file paths with different scores`() {
        addElement(10, "file1", "1")
        addElement(0, "file2", "123")
        addElement(0, "file3", "12")
        addElement(0, "file4", "1234")

        runPrioritizedList()
        assertEquals(4, result.size)
        assertEquals("file1", result[0].filename)
        assertEquals("file3", result[1].filename)
        assertEquals("file2", result[2].filename)
        assertEquals("file4", result[3].filename)
    }

    @Test
    fun `Prioritize empty paths`() {
        addElement(4, "file1", "")
        addElement(3, "file2", "")
        addElement(1, "file3", "")
        addElement(2, "file4", "")

        runPrioritizedList()
        assertEquals(4, result.size)
        assertEquals("file1", result[0].filename)
        assertEquals("file2", result[1].filename)
        assertEquals("file4", result[2].filename)
        assertEquals("file3", result[3].filename)
    }

    @Test
    fun `Test ignored characters`() {
        val searchString = "HELLO/THERE/GENERAL/KENOBI"
        val ignoredChars = "H/"
        assertEquals("elloteregeneralkenobi", FuzzierUtil.cleanSearchString(searchString, ignoredChars))
    }

    @Test
    fun `No ignored characters`() {
        val searchString = "!#¤%(&`Soqwe'"
        val ignoredChars = ""
        assertEquals(searchString.lowercase(), FuzzierUtil.cleanSearchString(searchString, ignoredChars))
    }

    private fun addElement(score: Int, fileName: String) {
        val fuzzyScore = FuzzyScore()
        fuzzyScore.streakScore = score
        val container = FuzzyMatchContainer(fuzzyScore, "", fileName, "")
        listModel.addElement(container)
    }

    private fun addElement(score: Int, filename: String, filePath: String) {
        val fuzzyScore = FuzzyScore()
        fuzzyScore.streakScore = score
        val container = FuzzyMatchContainer(fuzzyScore, filePath, filename, "")
        listModel.addElement(container)
    }

    private fun runPrioritizedList() {
        fuzzierUtil.setListLimit(4)
        fuzzierUtil.setPrioritizeShorterDirPaths(true)
        result = fuzzierUtil.sortAndLimit(listModel, true)
    }

    /**
     * Tests both the dir sort without priority and the file sorting
     */
    private fun runWithLimit(limit: Int) {
        fuzzierUtil.setListLimit(limit)
        fuzzierUtil.setPrioritizeShorterDirPaths(false)
        val dirResult = fuzzierUtil.sortAndLimit(listModel, true)
        result = fuzzierUtil.sortAndLimit(listModel)

        assertEquals(dirResult.size, result.size)

        var i = 0
        while (i < result.size) {
            assertEquals(result[i], dirResult[i])
            i++
        }
    }
}