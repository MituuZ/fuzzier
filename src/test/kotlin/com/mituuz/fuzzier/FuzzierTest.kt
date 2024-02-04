package com.mituuz.fuzzier

import com.intellij.openapi.components.service
import com.intellij.testFramework.TestApplicationManager
import com.mituuz.fuzzier.settings.FuzzierSettingsService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class FuzzierTest {
    private var fuzzier: Fuzzier
    private var testApplicationManager: TestApplicationManager
    private var settings: FuzzierSettingsService.State

    init {
        testApplicationManager = TestApplicationManager.getInstance()
        fuzzier = Fuzzier()
        settings  = service<FuzzierSettingsService>().state
    }

    @Test
    fun fuzzyScoreEmptyString() {
        val results = listOf(0,
            0, // 1 streak + 4 chars (0.5)
            0, // 1 streak + 4 chars (1)
            0,
            0) // 1 streak (5)

        for (i in 0..4) {
            runSettings(i)
            val match = fuzzier.fuzzyContainsCaseInsensitive("", "")
            assertMatch(results[i], match)
        }
    }

    @Test
    fun fuzzyScoreNoStreak() {
        val results = listOf(1,
            3, // 1 streak + 4 chars (0.5)
            5, // 1 streak + 4 chars (1)
            1,
            5) // 1 streak (5)

        for (i in 0..4) {
            runSettings(i)
            val match = fuzzier.fuzzyContainsCaseInsensitive("KotlinIsFun", "kif")
            assertMatch(results[i], match)
        }
    }

    @Test
    fun fuzzyScoreStreak() {
        default()
        val match = fuzzier.fuzzyContainsCaseInsensitive("KotlinIsFun", "kot")
        assertMatch(3, match)
    }

    @Test
    fun fuzzyScoreLongSearchString() {
        default()
        val match = fuzzier.fuzzyContainsCaseInsensitive("KIF", "TooLongSearchString")
        assertNull(match)
    }

    @Test
    fun fuzzyScoreNoPossibleMatch() {
        default()
        val match = fuzzier.fuzzyContainsCaseInsensitive("KIF", "A")
        assertNull(match)
    }

    @Test
    fun fuzzyScoreNoPossibleMatchSplit() {
        default()
        val match = fuzzier.fuzzyContainsCaseInsensitive("Kotlin/Is/Fun/kif.kt", "A A B")
        assertNull(match)
    }

    @Test
    fun fuzzyScorePartialMatchSplit() {
        default()
        val match = fuzzier.fuzzyContainsCaseInsensitive("Kotlin/Is/Fun/kif.kt", "A A K")
        assertNull(match)
    }

    @Test
    fun fuzzyScoreFilePathMatch() {
        default()
        var match = fuzzier.fuzzyContainsCaseInsensitive("Kotlin/Is/Fun/kif.kt", "kif")
        assertMatch(11, match)

        match = fuzzier.fuzzyContainsCaseInsensitive("Kiffer/Is/Fun/kif.kt", "kif")
        assertMatch(13, match)

        match = fuzzier.fuzzyContainsCaseInsensitive("Kiffer/Is/Fun/kiffer.kt", "kif")
        assertMatch(3, match)

        match = fuzzier.fuzzyContainsCaseInsensitive("Kotlin/Is/Fun/kif.kt", "kt")
        assertMatch(11, match)

        match = fuzzier.fuzzyContainsCaseInsensitive("Kif/Is/Fun/kif.kt", "kif")
        assertMatch(23, match)

        match = fuzzier.fuzzyContainsCaseInsensitive("Kotlin/Is/Fun/kif.kt", "kif fun kotlin")
        assertMatch(40, match)

        match = fuzzier.fuzzyContainsCaseInsensitive("Kotlin/Is/Fun/kif.kt", "is kt")
        assertMatch(22, match)
    }

    @Test
    fun fuzzyScoreSpaceMatch() {
        default()
        val match = fuzzier.fuzzyContainsCaseInsensitive("Kotlin/Is/Fun/kif.kt", "fun kotlin")
        assertMatch(29, match)
    }

    private fun runSettings(selector: Int) {
        print("Running with settings: $selector\n")
        when (selector) {
            0 -> default()
            1 -> multiMatch()
            2 -> multiMatchSingleCharScore()
            3 -> partialPath()
            4 -> streakModifier()
        }
    }

    private fun default() {
        settings.multiMatch = false
        settings.matchWeightSingleChar = 5 // Not active because of multi match
        settings.matchWeightPartialPath = 10
        settings.matchWeightStreakModifier = 10
        fuzzier.setSettings()
    }

    private fun multiMatch() {
        settings.multiMatch = true
        settings.matchWeightSingleChar = 5
        settings.matchWeightPartialPath = 10
        settings.matchWeightStreakModifier = 10
        fuzzier.setSettings()
    }

    private fun multiMatchSingleCharScore() {
        settings.multiMatch = true
        settings.matchWeightSingleChar = 10
        settings.matchWeightPartialPath = 10
        settings.matchWeightStreakModifier = 10
        fuzzier.setSettings()
    }

    private fun partialPath() {
        settings.multiMatch = false
        settings.matchWeightSingleChar = 5
        settings.matchWeightPartialPath = 50
        settings.matchWeightStreakModifier = 10
        fuzzier.setSettings()
    }

    private fun streakModifier() {
        settings.multiMatch = false
        settings.matchWeightSingleChar = 5
        settings.matchWeightPartialPath = 10
        settings.matchWeightStreakModifier = 50
        fuzzier.setSettings()
    }

    private fun assertMatch(score: Int, container: Fuzzier.FuzzyMatchContainer?) {
        if (container != null) {
            assertEquals(score, container.score)
        } else {
            fail("match is null")
        }
    }
}