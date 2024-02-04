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
    private lateinit var results: List<Int>

    init {
        testApplicationManager = TestApplicationManager.getInstance()
        fuzzier = Fuzzier()
        settings  = service<FuzzierSettingsService>().state
    }

    @Test
    fun fuzzyScoreEmptyString() {
        results = listOf(
            0,
            0,
            0,
            0,
            0
        )

        runTests("", "")
    }

    @Test
    fun fuzzyScoreNoStreak() {
        results = listOf(
            1, // 1 streak
            3, // 1 streak + 4 chars (0.5)
            5, // 1 streak + 4 chars (1)
            1, // no partial path
            5 // 1 streak (5)
        )

        runTests("KotlinIsFun", "kif")
    }

    @Test
    fun fuzzyScoreStreak() {
        results = listOf(
            3, // 3 streak
            4, // 3 streak + 3 chars (0.5)
            6, // 3 streak + 3 chars (1)
            3, // no partial path
            15 // 3 streak (5)
        )

        runTests("KotlinIsFun", "kot")
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

    private fun runTests(filePath: String, searchString: String) {
        for (i in 0..4) {
            runSettings(i)
            val match = fuzzier.fuzzyContainsCaseInsensitive(filePath, searchString)
            assertMatch(results[i], match)
        }
    }

    private fun runSettings(selector: Int) {
        print("Running with settings: $selector: ")
        when (selector) {
            0 -> default()
            1 -> multiMatch()
            2 -> multiMatchSingleCharScore()
            3 -> partialPath()
            4 -> streakModifier()
        }
    }

    private fun default() {
        print("Default\n")
        settings.multiMatch = false
        settings.matchWeightSingleChar = 5 // Not active because of multi match
        settings.matchWeightPartialPath = 10
        settings.matchWeightStreakModifier = 10
        fuzzier.setSettings()
    }

    private fun multiMatch() {
        print("MultiMatch\n")
        settings.multiMatch = true
        settings.matchWeightSingleChar = 5
        settings.matchWeightPartialPath = 10
        settings.matchWeightStreakModifier = 10
        fuzzier.setSettings()
    }

    private fun multiMatchSingleCharScore() {
        print("MultiMatchSingleCharScore\n")
        settings.multiMatch = true
        settings.matchWeightSingleChar = 10
        settings.matchWeightPartialPath = 10
        settings.matchWeightStreakModifier = 10
        fuzzier.setSettings()
    }

    private fun partialPath() {
        print("PartialPath\n")
        settings.multiMatch = false
        settings.matchWeightSingleChar = 5
        settings.matchWeightPartialPath = 50
        settings.matchWeightStreakModifier = 10
        fuzzier.setSettings()
    }

    private fun streakModifier() {
        print("StreakModifier\n")
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