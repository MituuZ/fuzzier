package com.mituuz.fuzzier

import com.intellij.testFramework.TestApplicationManager
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class ExcludeTest {
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
        val filePathContainer = testUtil.setUpProjectFileIndex(filePaths, listOf("asd", "nope"))
        Assertions.assertEquals(1, filePathContainer.size())
        Assertions.assertEquals("/main.kt", filePathContainer.get(0).string)
    }

    @Test
    fun excludeListTestNoMatches() {
        val filePaths = listOf("src/main.kt", "src/not.kt", "src/dsa/not.kt")
        val filePathContainer = testUtil.setUpProjectFileIndex(filePaths, listOf("asd"))
        Assertions.assertEquals(3, filePathContainer.size())
        Assertions.assertEquals("/main.kt", filePathContainer.get(2).string)
        Assertions.assertEquals("/not.kt", filePathContainer.get(1).string)
        Assertions.assertEquals("/dsa/not.kt", filePathContainer.get(0).string)
    }

    @Test
    fun excludeListTestEmptyList() {
        val filePaths = listOf("src/main.kt", "src/not.kt", "src/dsa/not.kt")
        val filePathContainer = testUtil.setUpProjectFileIndex(filePaths, ArrayList())
        Assertions.assertEquals(3, filePathContainer.size())
        Assertions.assertEquals("/main.kt", filePathContainer.get(2).string)
        Assertions.assertEquals("/not.kt", filePathContainer.get(1).string)
        Assertions.assertEquals("/dsa/not.kt", filePathContainer.get(0).string)
    }

    @Test
    fun excludeListTestStartsWith() {
        val filePaths = listOf("src/main.kt", "src/asd/main.kt", "src/asd/asd.kt", "src/not/asd.kt")
        val filePathContainer = testUtil.setUpProjectFileIndex(filePaths, listOf("/asd*"))
        Assertions.assertEquals(2, filePathContainer.size())
        Assertions.assertEquals("/not/asd.kt", filePathContainer.get(0).string)
    }

    @Test
    fun excludeListTestEndsWith() {
        val filePaths = listOf("src/main.log", "src/asd/main.log", "src/asd/asd.kt", "src/not/asd.kt", "src/nope")
        val filePathContainer = testUtil.setUpProjectFileIndex(filePaths, listOf("*.log"))
        Assertions.assertEquals(3, filePathContainer.size())
        Assertions.assertEquals("/asd/asd.kt", filePathContainer.get(0).string)
    }
}