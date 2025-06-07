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
package com.mituuz.fuzzier

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.File

class ExcludeIgnoreTest {
    private var testUtil = TestUtil()
    private val separator = File.separator

    @Test
    fun excludeListTest() {
        val filePaths = listOf(
            "src" + separator + "main.kt",
            "src" + separator + "asd" + separator + "main.kt",
            "src" + separator + "asd" + separator + "asd.kt",
            "src" + separator + "not" + separator + "asd.kt",
            "src" + separator + "nope"
        )
        val filePathContainer = testUtil.setUpModuleFileIndex(filePaths, setOf("asd", "nope"))
        Assertions.assertEquals(1, filePathContainer.size())
        Assertions.assertEquals("" + separator + "main.kt", filePathContainer.get(0).filePath)
    }

    @Test
    fun excludeListTestNoMatches() {
        val filePaths = listOf(
            "src" + separator + "main.kt",
            "src" + separator + "not.kt",
            "src" + separator + "dsa" + separator + "not.kt"
        )
        val filePathContainer = testUtil.setUpModuleFileIndex(filePaths, setOf("asd"))
        Assertions.assertEquals(3, filePathContainer.size())
        Assertions.assertEquals("" + separator + "main.kt", filePathContainer.get(2).filePath)
        Assertions.assertEquals("" + separator + "not.kt", filePathContainer.get(1).filePath)
        Assertions.assertEquals("" + separator + "dsa" + separator + "not.kt", filePathContainer.get(0).filePath)
    }


    @Test
    fun excludeListTestEmptyList() {
        val filePaths = listOf(
            "src" + separator + "main.kt",
            "src" + separator + "not.kt",
            "src" + separator + "dsa" + separator + "not.kt"
        )
        val filePathContainer = testUtil.setUpModuleFileIndex(filePaths, setOf())
        Assertions.assertEquals(3, filePathContainer.size())
        Assertions.assertEquals("" + separator + "main.kt", filePathContainer.get(2).filePath)
        Assertions.assertEquals("" + separator + "not.kt", filePathContainer.get(1).filePath)
        Assertions.assertEquals("" + separator + "dsa" + separator + "not.kt", filePathContainer.get(0).filePath)
    }

    @Test
    fun excludeListTestStartsWith() {
        val filePaths = listOf(
            "src" + separator + "main.kt",
            "src" + separator + "asd" + separator + "main.kt",
            "src" + separator + "asd" + separator + "asd.kt",
            "src" + separator + "not" + separator + "asd.kt"
        )
        val filePathContainer = testUtil.setUpModuleFileIndex(filePaths, setOf("" + separator + "asd*"))
        Assertions.assertEquals(2, filePathContainer.size())
        Assertions.assertEquals("" + separator + "not" + separator + "asd.kt", filePathContainer.get(0).filePath)
    }

    @Test
    fun excludeListTestEndsWith() {
        val filePaths = listOf(
            "src" + separator + "main.log",
            "src" + separator + "asd" + separator + "main.log",
            "src" + separator + "asd" + separator + "asd.kt",
            "src" + separator + "not" + separator + "asd.kt",
            "src" + separator + "nope"
        )
        val filePathContainer = testUtil.setUpModuleFileIndex(filePaths, setOf("*.log"))
        Assertions.assertEquals(3, filePathContainer.size())
        Assertions.assertEquals("" + separator + "asd" + separator + "asd.kt", filePathContainer.get(0).filePath)
    }

    @Test
    fun testIgnoreOneFile() {
        val filePaths = listOf("src" + separator + "ignore-me.kt", "src" + separator + "main.kt")
        val filePathContainer =
            testUtil.setUpModuleFileIndex(filePaths, setOf(), listOf("src" + separator + "ignore-me.kt"))
        Assertions.assertEquals(1, filePathContainer.size())
        Assertions.assertEquals("" + separator + "main.kt", filePathContainer.get(0).filePath)
    }

    @Test
    fun testIgnoreEmptyList() {
        val filePaths = listOf(
            "src" + separator + "dir" + separator + "file.txt",
            "src" + separator + "main.kt",
            "src" + separator + "other.kt"
        )
        val filePathContainer = testUtil.setUpModuleFileIndex(filePaths, setOf(), listOf())
        Assertions.assertEquals(3, filePathContainer.size())
        Assertions.assertEquals("" + separator + "dir" + separator + "file.txt", filePathContainer.get(0).filePath)
        Assertions.assertEquals("" + separator + "main.kt", filePathContainer.get(1).filePath)
        Assertions.assertEquals("" + separator + "other.kt", filePathContainer.get(2).filePath)
    }

    @Test
    fun testIgnoreMultipleFiles() {
        val filePaths = listOf(
            "src" + separator + "dir" + separator + "file.txt",
            "src" + separator + "main.kt",
            "src" + separator + "other.kt"
        )
        val filePathContainer = testUtil.setUpModuleFileIndex(
            filePaths,
            setOf(),
            listOf("src" + separator + "dir" + separator + "file.txt", "src" + separator + "other.kt")
        )
        Assertions.assertEquals(1, filePathContainer.size())
        Assertions.assertEquals("" + separator + "main.kt", filePathContainer.get(0).filePath)
    }

    @Test
    fun testIgnoreInCombinationWithExclusionList() {
        /* for a FuzzierVCS action only the ignore list should be applied, and the exclusions should be skipped */
        val filePaths = listOf(
            "src" + separator + "dir" + separator + "file.txt",
            "src" + separator + "main.kt",
            "src" + separator + "other.kt",
            "src" + separator + "ignore-me.kt",
            "src" + separator + "exclude-me.kt"
        )
        val filePathContainer = testUtil.setUpModuleFileIndex(
            filePaths,
            setOf("dir", "exclude-me.kt"),
            listOf("src" + separator + "ignore-me.kt")
        )
        Assertions.assertEquals(4, filePathContainer.size())
        Assertions.assertEquals("" + separator + "dir" + separator + "file.txt", filePathContainer.get(0).filePath)
        Assertions.assertEquals("" + separator + "main.kt", filePathContainer.get(1).filePath)
        Assertions.assertEquals("" + separator + "other.kt", filePathContainer.get(2).filePath)
        Assertions.assertEquals("" + separator + "exclude-me.kt", filePathContainer.get(3).filePath)
    }
}