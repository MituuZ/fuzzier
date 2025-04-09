/*
MIT License

Copyright (c) 2025 Mitja Leino

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/
package com.mituuz.fuzzier.entities

import com.intellij.testFramework.TestApplicationManager
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ScoreCalculatorTest {
    @Suppress("unused") // Required for the tests
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

        sc.setMatchWeightStreakModifier(10)
        val fScore = sc.calculateScore("/test")
        assertEquals(4, fScore!!.streakScore)
    }

    @Test
    fun `Basic streak longer path`() {
        val sc = ScoreCalculator("test")

        sc.setMatchWeightStreakModifier(10)
        val fScore = sc.calculateScore("/te/st")
        assertEquals(2, fScore!!.streakScore)
    }

    @Test
    fun `Basic streak no possible match`() {
        val sc = ScoreCalculator("test")

        sc.setMatchWeightStreakModifier(10)
        val fScore = sc.calculateScore("/te")
        assertNull(fScore)
    }

    @Test
    fun `Multi match basic test`() {
        val sc = ScoreCalculator("test")

        sc.setMultiMatch(true)
        sc.setMatchWeightSingleChar(10)
        val fScore = sc.calculateScore("/test")
        assertEquals(4, fScore!!.multiMatchScore)
    }

    @Test
    fun `Multi match basic test multiples`() {
        val sc = ScoreCalculator("test")

        sc.setMultiMatch(true)
        sc.setMatchWeightSingleChar(10)
        val fScore = sc.calculateScore("/testtest")
        assertEquals(8, fScore!!.multiMatchScore)
    }

    @Test
    fun `Multi match basic test multiples multiples`() {
        val sc = ScoreCalculator("test test")

        sc.setMultiMatch(true)
        sc.setMatchWeightSingleChar(10)
        val fScore = sc.calculateScore("/testtest")
        assertEquals(8, fScore!!.multiMatchScore)
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

        sc.setFilenameMatchWeight(10)
        val fScore = sc.calculateScore("/test.kt")
        assertEquals(4, fScore!!.filenameScore)
    }

    @Test
    fun `Empty ss and fp`() {
        val sc = ScoreCalculator("")

        val fScore = sc.calculateScore("")
        assertEquals(0, fScore!!.getTotalScore())
    }

    // Legacy tests
    @Test
    fun `Non-consecutive streak`() {
        val sc = ScoreCalculator("kif")

        sc.setMatchWeightStreakModifier(10)
        sc.setMultiMatch(true)
        sc.setMatchWeightSingleChar(10)
        sc.setFilenameMatchWeight(10)

        val fScore = sc.calculateScore("/KotlinIsFun")

        assertEquals(1, fScore!!.streakScore)
        assertEquals(4, fScore.multiMatchScore)
        assertEquals(0, fScore.partialPathScore)
        assertEquals(1, fScore.filenameScore)
    }

    @Test
    fun `Consecutive streak`() {
        val sc = ScoreCalculator("kot")

        sc.setMatchWeightStreakModifier(10)
        sc.setMultiMatch(true)
        sc.setMatchWeightSingleChar(10)
        sc.setFilenameMatchWeight(10)

        val fScore = sc.calculateScore("/KotlinIsFun")

        assertEquals(3, fScore!!.streakScore)
        assertEquals(3, fScore.multiMatchScore)
        assertEquals(0, fScore.partialPathScore)
        assertEquals(3, fScore.filenameScore)
    }

    @Test
    fun `Too long ss`() {
        val sc = ScoreCalculator("TooLongSearchString")
        val fScore = sc.calculateScore("/KIF")
        assertNull(fScore)
    }

    @Test
    fun `No possible match`() {
        val sc = ScoreCalculator("A")
        val fScore = sc.calculateScore("/KIF")
        assertNull(fScore)
    }

    @Test
    fun `Empty ss`() {
        val sc = ScoreCalculator("")

        val fScore = sc.calculateScore("/KIF")
        assertEquals(0, fScore!!.getTotalScore())
    }

    @Test
    fun `No possible match split`() {
        val sc = ScoreCalculator("A A B")
        val fScore = sc.calculateScore("/Kotlin/Is/Fun/kif.kt")
        assertNull(fScore)
    }

    @Test
    fun `Partial match split`() {
        val sc = ScoreCalculator("A A K")
        val fScore = sc.calculateScore("/Kotlin/Is/Fun/kif.kt")
        assertNull(fScore)
    }

    @Test
    fun `Split match for space`() {
        val sc = ScoreCalculator("fun kotlin")

        sc.setMatchWeightStreakModifier(10)
        sc.setMultiMatch(true)
        sc.setMatchWeightSingleChar(10)
        sc.setFilenameMatchWeight(10)

        val fScore = sc.calculateScore("/Kotlin/Is/Fun/kif.kt")

        assertEquals(6, fScore!!.streakScore)
        assertEquals(15, fScore.multiMatchScore)
        assertEquals(20, fScore.partialPathScore)
        assertEquals(1, fScore.filenameScore)
    }

    @Test
    fun `Legacy test 1`() {
        val sc = ScoreCalculator("kif")

        sc.setMatchWeightStreakModifier(10)
        sc.setMultiMatch(true)
        sc.setMatchWeightSingleChar(10)
        sc.setFilenameMatchWeight(10)

        val fScore = sc.calculateScore("/Kotlin/Is/Fun/kif.kt")

        assertEquals(1, fScore!!.streakScore)
        assertEquals(8, fScore.multiMatchScore)
        assertEquals(10, fScore.partialPathScore)
        assertEquals(3, fScore.filenameScore)
    }

    @Test
    fun `Legacy test 2`() {
        val sc = ScoreCalculator("kif")

        sc.setMatchWeightStreakModifier(10)
        sc.setMultiMatch(true)
        sc.setMatchWeightSingleChar(10)
        sc.setFilenameMatchWeight(10)

        val fScore = sc.calculateScore("/Kiffer/Is/Fun/kiffer.kt")

        assertEquals(3, fScore!!.streakScore)
        assertEquals(11, fScore.multiMatchScore)
        assertEquals(0, fScore.partialPathScore)
        assertEquals(3, fScore.filenameScore)
    }

    @Test
    fun `Multiple partial file path matches`() {
        val sc = ScoreCalculator("kif")

        sc.setMatchWeightStreakModifier(10)
        sc.setMultiMatch(true)
        sc.setMatchWeightSingleChar(10)
        sc.setFilenameMatchWeight(10)

        val fScore = sc.calculateScore("/Kif/Is/Fun/kif.kt")

        assertEquals(3, fScore!!.streakScore)
        assertEquals(9, fScore.multiMatchScore)
        assertEquals(20, fScore.partialPathScore)
        assertEquals(3, fScore.filenameScore)
    }

    @Test
    fun `Legacy test 3`() {
        val sc = ScoreCalculator("kif fun kotlin")

        sc.setMatchWeightStreakModifier(10)
        sc.setMultiMatch(true)
        sc.setMatchWeightSingleChar(10)
        sc.setFilenameMatchWeight(10)

        val fScore = sc.calculateScore("/Kotlin/Is/Fun/kif.kt")

        assertEquals(6, fScore!!.streakScore)
        assertEquals(15, fScore.multiMatchScore)
        assertEquals(30, fScore.partialPathScore)
        assertEquals(3, fScore.filenameScore)
    }

    @Test
    fun `5 tolerance matching normal match`() {
        val sc = ScoreCalculator("kotlin")
        sc.setTolerance(5)

        assertNotNull(sc.calculateScore("/Kotlin"))
    }

    @Test
    fun `5 tolerance matching 1 letter difference`() {
        val sc = ScoreCalculator("korlin")
        sc.setTolerance(5)

        assertNotNull(sc.calculateScore("/Kotlin"))
    }

    @Test
    fun `1 tolerance matching 1 letter difference`() {
        val sc = ScoreCalculator("korlin")
        sc.setTolerance(1)

        assertNotNull(sc.calculateScore("/Kotlin"))
    }

    @Test
    fun `1 tolerance matching 2 letter difference`() {
        val sc = ScoreCalculator("korlnn")
        sc.setTolerance(1)

        assertNull(sc.calculateScore("/Kotlin"))
    }

    @Test
    fun `1 tolerance matching 1 letter difference with split path`() {
        val sc = ScoreCalculator("korlin")
        sc.setTolerance(1)

        assertNotNull(sc.calculateScore("/Kot/lin"))
    }

    @Test
    fun `1 tolerance matching 2 letter difference with split path`() {
        val sc = ScoreCalculator("korlin")
        sc.setTolerance(1)

        assertNull(sc.calculateScore("/Kot/sin"))
    }

    @Test
    fun `2 tolerance matching 2 letter difference with split path`() {
        val sc = ScoreCalculator("korlin")
        sc.setTolerance(2)

        assertNotNull(sc.calculateScore("/Kot/sin"))
    }

    @Test
    fun `Don't match longer strings even if there is tolerance left`() {
        val sc = ScoreCalculator("kotlin12345")
        sc.setTolerance(5)

        assertNull(sc.calculateScore("/Kotlin"))
    }
}