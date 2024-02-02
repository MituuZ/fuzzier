package com.mituuz.fuzzier

import com.intellij.testFramework.TestApplicationManager
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
        val match = fuzzier.fuzzyContainsCaseInsensitive("", "", false)
        assertMatch(0, match)
    }

    @Test
    fun fuzzyScoreNoStreak() {
        val match = fuzzier.fuzzyContainsCaseInsensitive("KotlinIsFun", "kif", false)
        assertMatch(1, match)
    }

    @Test
    fun fuzzyScoreStreak() {
        val match = fuzzier.fuzzyContainsCaseInsensitive("KotlinIsFun", "kot", false)
        assertMatch(3, match)
    }

    @Test
    fun fuzzyScoreLongSearchString() {
        val match = fuzzier.fuzzyContainsCaseInsensitive("KIF", "TooLongSearchString", false)
        assertNull(match)
    }

    @Test
    fun fuzzyScoreNoPossibleMatch() {
        val match = fuzzier.fuzzyContainsCaseInsensitive("KIF", "A", false)
        assertNull(match)
    }

    @Test
    fun fuzzyScoreNoPossibleMatchSplit() {
        val match = fuzzier.fuzzyContainsCaseInsensitive("Kotlin/Is/Fun/kif.kt", "A A B", false)
        assertNull(match)
    }

    @Test
    fun fuzzyScorePartialMatchSplit() {
        val match = fuzzier.fuzzyContainsCaseInsensitive("Kotlin/Is/Fun/kif.kt", "A A K", false)
        assertNull(match)
    }

    @Test
    fun fuzzyScoreFilePathMatch() {
        var match = fuzzier.fuzzyContainsCaseInsensitive("Kotlin/Is/Fun/kif.kt", "kif", false)
        assertMatch(11, match)

        match = fuzzier.fuzzyContainsCaseInsensitive("Kiffer/Is/Fun/kif.kt", "kif", false)
        assertMatch(13, match)

        match = fuzzier.fuzzyContainsCaseInsensitive("Kiffer/Is/Fun/kiffer.kt", "kif", false)
        assertMatch(3, match)

        match = fuzzier.fuzzyContainsCaseInsensitive("Kotlin/Is/Fun/kif.kt", "kt", false)
        assertMatch(11, match)

        match = fuzzier.fuzzyContainsCaseInsensitive("Kif/Is/Fun/kif.kt", "kif", false)
        assertMatch(23, match)

        match = fuzzier.fuzzyContainsCaseInsensitive("Kotlin/Is/Fun/kif.kt", "kif fun kotlin", false)
        assertMatch(40, match)

        match = fuzzier.fuzzyContainsCaseInsensitive("Kotlin/Is/Fun/kif.kt", "is kt", false)
        assertMatch(22, match)
    }

    @Test
    fun fuzzyScoreSpaceMatch() {
        val match = fuzzier.fuzzyContainsCaseInsensitive("Kotlin/Is/Fun/kif.kt", "fun kotlin", false)
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