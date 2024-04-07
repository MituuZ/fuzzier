package com.mituuz.fuzzier.util

import com.intellij.testFramework.TestApplicationManager
import com.mituuz.fuzzier.entities.FuzzyMatchContainer
import com.mituuz.fuzzier.entities.FuzzyMatchContainer.FuzzyScore
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import javax.swing.DefaultListModel

class FuzzierUtilTest {
    @Suppress("unused")
    private val testApplicationManager = TestApplicationManager.getInstance()
    private val fuzzierUtil = FuzzierUtil()
    private val listModel = DefaultListModel<FuzzyMatchContainer>()
    private lateinit var result: DefaultListModel<FuzzyMatchContainer>

    @BeforeEach
    fun setUp() {
        listModel.clear()
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

    private fun addElement(score: Int, fileName: String) {
        val fuzzyScore = FuzzyScore()
        fuzzyScore.streakScore = score
        val container = FuzzyMatchContainer(fuzzyScore, "", fileName)
        listModel.addElement(container)
    }

    private fun runWithLimit(limit: Int) {
        fuzzierUtil.setListLimit(limit)
        result = fuzzierUtil.sortAndLimit(listModel)
    }
}