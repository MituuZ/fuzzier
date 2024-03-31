package com.mituuz.fuzzier

import com.intellij.openapi.components.service
import com.intellij.testFramework.TestApplicationManager
import com.mituuz.fuzzier.entities.FuzzyMatchContainer
import com.mituuz.fuzzier.settings.FuzzierSettingsService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class FuzzierTest {
    private var fuzzier: Fuzzier
    private var testApplicationManager: TestApplicationManager
    private var settings: FuzzierSettingsService.State
    private lateinit var results: List<Int>
    private val stringEvaluator = StringEvaluator(true, setOf(), 5, 10, 10)

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
            3, // 1 streak + 4 chars (0.5 rounded down)
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
        val match = stringEvaluator.fuzzyContainsCaseInsensitive("KIF", "TooLongSearchString")
        assertNull(match)
    }

    @Test
    fun fuzzyScoreNoPossibleMatch() {
        default()
        val match = stringEvaluator.fuzzyContainsCaseInsensitive("KIF", "A")
        assertNull(match)
    }

    @Test
    fun fuzzyScoreNoPossibleMatchSplit() {
        default()
        val match = stringEvaluator.fuzzyContainsCaseInsensitive("Kotlin/Is/Fun/kif.kt", "A A B")
        assertNull(match)
    }

    @Test
    fun fuzzyScorePartialMatchSplit() {
        default()
        val match = stringEvaluator.fuzzyContainsCaseInsensitive("Kotlin/Is/Fun/kif.kt", "A A K")
        assertNull(match)
    }

    @Test
    fun fuzzyScoreFilePathMatch() {
        default()
        var match = stringEvaluator.fuzzyContainsCaseInsensitive("Kotlin/Is/Fun/kif.kt", "kif")
        assertMatch(11, match)

        match = stringEvaluator.fuzzyContainsCaseInsensitive("Kiffer/Is/Fun/kif.kt", "kif")
        assertMatch(13, match)

        match = stringEvaluator.fuzzyContainsCaseInsensitive("Kiffer/Is/Fun/kiffer.kt", "kif")
        assertMatch(3, match)

        match = stringEvaluator.fuzzyContainsCaseInsensitive("Kotlin/Is/Fun/kif.kt", "kt")
        assertMatch(11, match)

        match = stringEvaluator.fuzzyContainsCaseInsensitive("Kif/Is/Fun/kif.kt", "kif")
        assertMatch(23, match)

        match = stringEvaluator.fuzzyContainsCaseInsensitive("Kotlin/Is/Fun/kif.kt", "kif fun kotlin")
        assertMatch(40, match)

        match = stringEvaluator.fuzzyContainsCaseInsensitive("Kotlin/Is/Fun/kif.kt", "is kt")
        assertMatch(22, match)
    }

    @Test
    fun fuzzyScoreSpaceMatch() {
        results = listOf(
            29, // 6 streak + 3 streak + 2x 10 partial matches
            36, // 6 streak + 2 streak + 2x 10 partial matches + 5 (0.5) rounded down (2) + match 12 (0.5) (6)
            45, // 6 streak + 2 streak + 2x 10 partial matches + 5 (1) + match 12 (1)
            109, // 6 streak + 3 streak + 2x 50 partial matches
            65 // 6 streak (30) + 3 streak (15) + 2x 10 partial matches
        )

        runTests("Kotlin/Is/Fun/kif.kt", "fun kotlin")
    }

    private fun runTests(filePath: String, searchString: String) {
        for (i in 0..4) {
            runSettings(i)
            val match = stringEvaluator.fuzzyContainsCaseInsensitive(filePath, searchString)
            assertMatch(results[i], match)
        }
    }

    private fun runSettings(selector: Int) {
        print("Running with settings: $selector: ")
        when (selector) {
            0 -> stringEvaluator.setSettings(false, 5, 10, 10)
            1 -> stringEvaluator.setSettings(true, 5, 10, 10)
            2 -> stringEvaluator.setSettings(true, 10, 10, 10)
            3 -> stringEvaluator.setSettings(false, 5, 50, 10)
            4 -> stringEvaluator.setSettings(false, 5, 10, 50)
        }
    }

    private fun default() {
        stringEvaluator.setSettings(false, 5, 10, 10)
    }

    private fun assertMatch(score: Int, container: FuzzyMatchContainer?) {
        if (container != null) {
            assertEquals(score, container.score)
        } else {
            fail("match is null")
        }
    }
}