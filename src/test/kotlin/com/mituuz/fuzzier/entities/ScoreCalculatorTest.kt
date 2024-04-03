package com.mituuz.fuzzier.entities

import com.intellij.testFramework.TestApplicationManager
import com.mituuz.fuzzier.settings.FuzzierSettingsService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ScoreCalculatorTest {
    private var testApplicationManager: TestApplicationManager = TestApplicationManager.getInstance()

    @Test
    fun `Search string contained same index`() {
        val sc = ScoreCalculator("test")
        sc.searchStringIndex = 0
        sc.searchStringLength = 4

        sc.currentFilePath = "test"
        sc.filePathIndex = 0

        assertTrue(sc.canSearchStringBeContained())
    }

    @Test
    fun `Search string contained different index`() {
        val sc = ScoreCalculator("test")
        sc.searchStringIndex = 0
        sc.searchStringLength = 4

        sc.currentFilePath = "test"
        sc.filePathIndex = 1

        assertFalse(sc.canSearchStringBeContained())
    }

    @Test
    fun `Basic streak happy case`() {
        val sc = ScoreCalculator("test")

        sc.setMatchWeightStreakModifier(1)
        val fScore = sc.calculateScore("/test")
        assertEquals(4, fScore!!.streakScore)
    }

    @Test
    fun `Basic streak longer path`() {
        val sc = ScoreCalculator("test")

        sc.setMatchWeightStreakModifier(1)
        val fScore = sc.calculateScore("/te/st")
        assertEquals(2, fScore!!.streakScore)
    }

    @Test
    fun `Basic streak no possible match`() {
        val sc = ScoreCalculator("test")

        sc.setMatchWeightStreakModifier(1)
        val fScore = sc.calculateScore("/te")
        assertNull(fScore)
    }

    // TODO: Remember to scale the weights to support ints!

    @Test
    fun `Multi match basic test`() {
        val sc = ScoreCalculator("test")

        sc.setMultiMatch(true)
        sc.setMatchWeightSingleChar(1)
        val fScore = sc.calculateScore("/test")
        assertEquals(4, fScore!!.multiMatchScore)
    }

    @Test
    fun `Multi match basic test multiples`() {
        val sc = ScoreCalculator("test")

        sc.setMultiMatch(true)
        sc.setMatchWeightSingleChar(1)
        val fScore = sc.calculateScore("/testtest")
        assertEquals(8, fScore!!.multiMatchScore)
    }

    @Test
    fun `Multi match basic test multiples multiples`() {
        val sc = ScoreCalculator("test test")

        sc.setMultiMatch(true)
        sc.setMatchWeightSingleChar(1)
        val fScore = sc.calculateScore("/testtest")
        assertEquals(16, fScore!!.multiMatchScore)
    }

    @Test
    fun `Partial path score basic test`() {
        val sc = ScoreCalculator("test")

        sc.setMatchWeightPartialPath(1)
        val fScore = sc.calculateScore("/test.kt")
        assertEquals(1, fScore!!.partialPathScore)
    }

    @Test
    fun `Filename score basic test`() {
        val sc = ScoreCalculator("test")

        sc.setFilenameMatchWeight(1)
        val fScore = sc.calculateScore("/test.kt")
        assertEquals(4, fScore!!.filenameScore)
    }
}