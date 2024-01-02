package com.mituuz.fuzzier

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class FuzzierTest {
    private val fuzzier = Fuzzier()
    @Test
    fun fuzzyScoreEmptyString() {
        val match = fuzzier.fuzzyContainsCaseInsensitive("", "")
        assertMatch(0, match)
    }

    @Test
    fun fuzzyScoreNoStreak() {
        val match = fuzzier.fuzzyContainsCaseInsensitive("KotlinIsFun", "kif")
        assertMatch(1, match)
    }

    @Test
    fun fuzzyScoreStreak() {
        val match = fuzzier.fuzzyContainsCaseInsensitive("KotlinIsFun", "kot")
        assertMatch(3, match)
    }

    @Test
    fun fuzzyScoreNoPossibleMatch() {
        val match = fuzzier.fuzzyContainsCaseInsensitive("KIF", "TooLongSearchString")
        assertNull(match)
    }

    @Test
    fun fuzzyScoreFilePathMatch() {
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
    }

    private fun assertMatch(score: Int, container: Fuzzier.FuzzyMatchContainer?) {
        if (container != null) {
            assertEquals(score, container.score)
        } else {
            fail("match is null")
        }
    }
}