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
        defaultSettings()
        val match = fuzzier.fuzzyContainsCaseInsensitive("", "")
        assertMatch(0, match)
    }

    @Test
    fun fuzzyScoreNoStreak() {
        defaultSettings()
        val match = fuzzier.fuzzyContainsCaseInsensitive("KotlinIsFun", "kif")
        assertMatch(1, match)
    }

    @Test
    fun fuzzyScoreStreak() {
        defaultSettings()
        val match = fuzzier.fuzzyContainsCaseInsensitive("KotlinIsFun", "kot")
        assertMatch(3, match)
    }

    @Test
    fun fuzzyScoreLongSearchString() {
        defaultSettings()
        val match = fuzzier.fuzzyContainsCaseInsensitive("KIF", "TooLongSearchString")
        assertNull(match)
    }

    @Test
    fun fuzzyScoreNoPossibleMatch() {
        defaultSettings()
        val match = fuzzier.fuzzyContainsCaseInsensitive("KIF", "A")
        assertNull(match)
    }

    @Test
    fun fuzzyScoreNoPossibleMatchSplit() {
        defaultSettings()
        val match = fuzzier.fuzzyContainsCaseInsensitive("Kotlin/Is/Fun/kif.kt", "A A B")
        assertNull(match)
    }

    @Test
    fun fuzzyScorePartialMatchSplit() {
        defaultSettings()
        val match = fuzzier.fuzzyContainsCaseInsensitive("Kotlin/Is/Fun/kif.kt", "A A K")
        assertNull(match)
    }

    @Test
    fun fuzzyScoreFilePathMatch() {
        defaultSettings()
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
        defaultSettings()
        val match = fuzzier.fuzzyContainsCaseInsensitive("Kotlin/Is/Fun/kif.kt", "fun kotlin")
        assertMatch(29, match)
    }

    private fun defaultSettings() {
        settings.multiMatch = false
        settings.matchWeightSingleChar = 5 // Not active because of multi match
        settings.matchWeightPartialPath = 10
        settings.matchWeightStreakModifier = 10
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