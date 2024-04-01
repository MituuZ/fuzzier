package com.mituuz.fuzzier.entities

import com.intellij.testFramework.TestApplicationManager
import com.mituuz.fuzzier.settings.FuzzierSettingsService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ScoreCalculatorTest {
    private var testApplicationManager: TestApplicationManager = TestApplicationManager.getInstance()
    private var settings: FuzzierSettingsService.State = FuzzierSettingsService.State()

    private val sc = ScoreCalculator("test")

    @Test
    fun `Search string contained same index`() {
        sc.searchStringIndex = 0
        sc.searchStringLength = 4

        sc.currentFilePath = "test"
        sc.filePathIndex = 0

        assertTrue(sc.canSearchStringBeContained())
    }

    @Test
    fun `Search string contained different index`() {
        sc.searchStringIndex = 0
        sc.searchStringLength = 4

        sc.currentFilePath = "test"
        sc.filePathIndex = 1

        assertFalse(sc.canSearchStringBeContained())
    }

    @Test
    fun `Test the whole process`() {
        sc.setMatchWeightStreakModifier(1)
        val fScore = sc.calculateScore("/test", "test")
        assertEquals(4, fScore!!.streakScore)
    }
}