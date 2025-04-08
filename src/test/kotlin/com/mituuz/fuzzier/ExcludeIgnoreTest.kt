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

class ExcludeIgnoreTest {
    private var testUtil = TestUtil()

    @Test
    fun excludeListTest() {
        val filePaths = listOf("src/main.kt", "src/asd/main.kt", "src/asd/asd.kt", "src/not/asd.kt", "src/nope")
        val filePathContainer = testUtil.setUpModuleFileIndex(filePaths, setOf("asd", "nope"))
        Assertions.assertEquals(1, filePathContainer.size())
        Assertions.assertEquals("/main.kt", filePathContainer.get(0).filePath)
    }

    @Test
    fun excludeListTestNoMatches() {
        val filePaths = listOf("src/main.kt", "src/not.kt", "src/dsa/not.kt")
        val filePathContainer = testUtil.setUpModuleFileIndex(filePaths, setOf("asd"))
        Assertions.assertEquals(3, filePathContainer.size())
        Assertions.assertEquals("/main.kt", filePathContainer.get(2).filePath)
        Assertions.assertEquals("/not.kt", filePathContainer.get(1).filePath)
        Assertions.assertEquals("/dsa/not.kt", filePathContainer.get(0).filePath)
    }


    @Test
    fun excludeListTestEmptyList() {
        val filePaths = listOf("src/main.kt", "src/not.kt", "src/dsa/not.kt")
        val filePathContainer = testUtil.setUpModuleFileIndex(filePaths, setOf())
        Assertions.assertEquals(3, filePathContainer.size())
        Assertions.assertEquals("/main.kt", filePathContainer.get(2).filePath)
        Assertions.assertEquals("/not.kt", filePathContainer.get(1).filePath)
        Assertions.assertEquals("/dsa/not.kt", filePathContainer.get(0).filePath)
    }

    @Test
    fun excludeListTestStartsWith() {
        val filePaths = listOf("src/main.kt", "src/asd/main.kt", "src/asd/asd.kt", "src/not/asd.kt")
        val filePathContainer = testUtil.setUpModuleFileIndex(filePaths, setOf("/asd*"))
        Assertions.assertEquals(2, filePathContainer.size())
        Assertions.assertEquals("/not/asd.kt", filePathContainer.get(0).filePath)
    }

    @Test
    fun excludeListTestEndsWith() {
        val filePaths = listOf("src/main.log", "src/asd/main.log", "src/asd/asd.kt", "src/not/asd.kt", "src/nope")
        val filePathContainer = testUtil.setUpModuleFileIndex(filePaths, setOf("*.log"))
        Assertions.assertEquals(3, filePathContainer.size())
        Assertions.assertEquals("/asd/asd.kt", filePathContainer.get(0).filePath)
    }

    @Test
    fun testIgnoreOneFile() {
        val filePaths = listOf("src/ignore-me.kt", "src/main.kt")
        val filePathContainer = testUtil.setUpModuleFileIndex(filePaths, setOf(), listOf("src/ignore-me.kt"))
        Assertions.assertEquals(1, filePathContainer.size())
        Assertions.assertEquals("/main.kt", filePathContainer.get(0).filePath)
    }

    @Test
    fun testIgnoreEmptyList() {
        val filePaths = listOf("src/dir/file.txt", "src/main.kt", "src/other.kt")
        val filePathContainer = testUtil.setUpModuleFileIndex(filePaths, setOf(), listOf())
        Assertions.assertEquals(3, filePathContainer.size())
        Assertions.assertEquals("/dir/file.txt", filePathContainer.get(0).filePath)
        Assertions.assertEquals("/main.kt", filePathContainer.get(1).filePath)
        Assertions.assertEquals("/other.kt", filePathContainer.get(2).filePath)
    }

    @Test
    fun testIgnoreMultipleFiles() {
        val filePaths = listOf("src/dir/file.txt", "src/main.kt", "src/other.kt")
        val filePathContainer = testUtil.setUpModuleFileIndex(filePaths, setOf(), listOf("src/dir/file.txt", "src/other.kt"))
        Assertions.assertEquals(1, filePathContainer.size())
        Assertions.assertEquals("/main.kt", filePathContainer.get(0).filePath)
    }

    @Test
    fun testIgnoreInCombinationWithExclusionList() {
        /* for a FuzzierVCS action only the ignore list should be applied, and the exclusions should be skipped */
        val filePaths = listOf("src/dir/file.txt", "src/main.kt", "src/other.kt", "src/ignore-me.kt", "src/exclude-me.kt")
        val filePathContainer = testUtil.setUpModuleFileIndex(filePaths, setOf("dir", "exclude-me.kt"), listOf("src/ignore-me.kt"))
        Assertions.assertEquals(4, filePathContainer.size())
        Assertions.assertEquals("/dir/file.txt", filePathContainer.get(0).filePath)
        Assertions.assertEquals("/main.kt", filePathContainer.get(1).filePath)
        Assertions.assertEquals("/other.kt", filePathContainer.get(2).filePath)
        Assertions.assertEquals("/exclude-me.kt", filePathContainer.get(3).filePath)
    }
}