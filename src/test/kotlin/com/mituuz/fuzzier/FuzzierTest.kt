package com.mituuz.fuzzier

import com.intellij.openapi.components.service
import com.intellij.testFramework.TestApplicationManager
import com.mituuz.fuzzier.settings.FuzzierSettingsService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class FuzzierTest {
    private var fuzzier: Fuzzier
    private var testApplicationManager: TestApplicationManager

    init {
        testApplicationManager = TestApplicationManager.getInstance()
        fuzzier = Fuzzier()
    }

    @Test
    fun fuzzyScoreEmptyString() {
        service<FuzzierSettingsService>().state.multiMatch = false
        val match = fuzzier.fuzzyContainsCaseInsensitive("", "")
        assertMatch(0, match)
    }

    @Test
    fun fuzzyScoreNoStreak() {
        service<FuzzierSettingsService>().state.multiMatch = false
        val match = fuzzier.fuzzyContainsCaseInsensitive("KotlinIsFun", "kif")
        assertMatch(1, match)
    }

    @Test
    fun fuzzyScoreStreak() {
        service<FuzzierSettingsService>().state.multiMatch = false
        val match = fuzzier.fuzzyContainsCaseInsensitive("KotlinIsFun", "kot")
        assertMatch(3, match)
    }

    @Test
    fun fuzzyScoreLongSearchString() {
        service<FuzzierSettingsService>().state.multiMatch = false
        val match = fuzzier.fuzzyContainsCaseInsensitive("KIF", "TooLongSearchString")
        assertNull(match)
    }

    @Test
    fun fuzzyScoreNoPossibleMatch() {
        service<FuzzierSettingsService>().state.multiMatch = false
        val match = fuzzier.fuzzyContainsCaseInsensitive("KIF", "A")
        assertNull(match)
    }

    @Test
    fun fuzzyScoreNoPossibleMatchSplit() {
        service<FuzzierSettingsService>().state.multiMatch = false
        val match = fuzzier.fuzzyContainsCaseInsensitive("Kotlin/Is/Fun/kif.kt", "A A B")
        assertNull(match)
    }

    @Test
    fun fuzzyScorePartialMatchSplit() {
        service<FuzzierSettingsService>().state.multiMatch = false
        val match = fuzzier.fuzzyContainsCaseInsensitive("Kotlin/Is/Fun/kif.kt", "A A K")
        assertNull(match)
    }

    @Test
    fun fuzzyScoreFilePathMatch() {
        service<FuzzierSettingsService>().state.multiMatch = false
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
        service<FuzzierSettingsService>().state.multiMatch = false
        val match = fuzzier.fuzzyContainsCaseInsensitive("Kotlin/Is/Fun/kif.kt", "fun kotlin")
        assertMatch(29, match)
    }

    private fun assertMatch(score: Int, container: Fuzzier.FuzzyMatchContainer?) {
        if (container != null) {
            assertEquals(score, container.score)
        } else {
            fail("match is null")
        }
    }
}