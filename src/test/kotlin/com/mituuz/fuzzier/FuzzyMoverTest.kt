package com.mituuz.fuzzier

import com.intellij.testFramework.TestApplicationManager
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class FuzzyMoverTest {
    private val fuzzyMover: FuzzyMover = FuzzyMover()
    private val testApplicationManager: TestApplicationManager = TestApplicationManager.getInstance()
    private val testUtil: TestUtil = TestUtil()

    @Test
    fun `Check that files are moved correctly`() {
        val filePaths = listOf("src/main.log", "src/asd/main.log", "src/asd/asd.kt", "src/not/asd.kt", "src/nope")
        val filePathContainer = testUtil.setUpProjectFileIndex(filePaths, listOf("*.log"))

    }
}