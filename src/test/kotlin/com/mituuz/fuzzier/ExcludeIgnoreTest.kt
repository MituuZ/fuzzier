package com.mituuz.fuzzier

import com.intellij.testFramework.TestApplicationManager
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class ExcludeIgnoreTest {
    private var fuzzier: Fuzzier
    private var testApplicationManager: TestApplicationManager
    private var testUtil = TestUtil()

    init {
        testApplicationManager = TestApplicationManager.getInstance()
        fuzzier = Fuzzier()
    }

    @Test
    fun excludeListTest() {
        val filePaths = listOf("src/main.kt", "src/asd/main.kt", "src/asd/asd.kt", "src/not/asd.kt", "src/nope")
        val filePathContainer = testUtil.setUpProjectFileIndex(filePaths, setOf("asd", "nope"))
        Assertions.assertEquals(1, filePathContainer.size())
        Assertions.assertEquals("/main.kt", filePathContainer.get(0).filePath)
    }

    @Test
    fun excludeListTestNoMatches() {
        val filePaths = listOf("src/main.kt", "src/not.kt", "src/dsa/not.kt")
        val filePathContainer = testUtil.setUpProjectFileIndex(filePaths, setOf("asd"))
        Assertions.assertEquals(3, filePathContainer.size())
        Assertions.assertEquals("/main.kt", filePathContainer.get(2).filePath)
        Assertions.assertEquals("/not.kt", filePathContainer.get(1).filePath)
        Assertions.assertEquals("/dsa/not.kt", filePathContainer.get(0).filePath)
    }


    @Test
    fun excludeListTestEmptyList() {
        val filePaths = listOf("src/main.kt", "src/not.kt", "src/dsa/not.kt")
        val filePathContainer = testUtil.setUpProjectFileIndex(filePaths, setOf())
        Assertions.assertEquals(3, filePathContainer.size())
        Assertions.assertEquals("/main.kt", filePathContainer.get(2).filePath)
        Assertions.assertEquals("/not.kt", filePathContainer.get(1).filePath)
        Assertions.assertEquals("/dsa/not.kt", filePathContainer.get(0).filePath)
    }

    @Test
    fun excludeListTestStartsWith() {
        val filePaths = listOf("src/main.kt", "src/asd/main.kt", "src/asd/asd.kt", "src/not/asd.kt")
        val filePathContainer = testUtil.setUpProjectFileIndex(filePaths, setOf("/asd*"))
        Assertions.assertEquals(2, filePathContainer.size())
        Assertions.assertEquals("/not/asd.kt", filePathContainer.get(0).filePath)
    }

    @Test
    fun excludeListTestEndsWith() {
        val filePaths = listOf("src/main.log", "src/asd/main.log", "src/asd/asd.kt", "src/not/asd.kt", "src/nope")
        val filePathContainer = testUtil.setUpProjectFileIndex(filePaths, setOf("*.log"))
        Assertions.assertEquals(3, filePathContainer.size())
        Assertions.assertEquals("/asd/asd.kt", filePathContainer.get(0).filePath)
    }

    @Test
    fun testIgnoreOneFile() {
        val filePaths = listOf("src/ignore-me.kt", "src/main.kt")
        val filePathContainer = testUtil.setUpProjectFileIndex(filePaths, setOf(), listOf("src/ignore-me.kt"))
        Assertions.assertEquals(1, filePathContainer.size())
        Assertions.assertEquals("/main.kt", filePathContainer.get(0).filePath)
    }

    @Test
    fun testIgnoreEmptyList() {
        val filePaths = listOf("src/dir/file.txt", "src/main.kt", "src/other.kt")
        val filePathContainer = testUtil.setUpProjectFileIndex(filePaths, setOf(), listOf())
        Assertions.assertEquals(3, filePathContainer.size())
        Assertions.assertEquals("/dir/file.txt", filePathContainer.get(0).filePath)
        Assertions.assertEquals("/main.kt", filePathContainer.get(1).filePath)
        Assertions.assertEquals("/other.kt", filePathContainer.get(2).filePath)
    }

    @Test
    fun testIgnoreMultipleFiles() {
        val filePaths = listOf("src/dir/file.txt", "src/main.kt", "src/other.kt")
        val filePathContainer = testUtil.setUpProjectFileIndex(filePaths, setOf(), listOf("src/dir/file.txt", "src/other.kt"))
        Assertions.assertEquals(1, filePathContainer.size())
        Assertions.assertEquals("/main.kt", filePathContainer.get(0).filePath)
    }

    @Test
    fun testIgnoreInCombinationWithExclusionList() {
        val filePaths = listOf("src/dir/file.txt", "src/main.kt", "src/other.kt", "src/ignore-me.kt", "src/exclude-me.kt")
        val filePathContainer = testUtil.setUpProjectFileIndex(filePaths, setOf("dir", "exclude-me.kt"), listOf("src/ignore-me.kt"))
        Assertions.assertEquals(2, filePathContainer.size())
        Assertions.assertEquals("/main.kt", filePathContainer.get(0).filePath)
        Assertions.assertEquals("/other.kt", filePathContainer.get(1).filePath)
    }
}