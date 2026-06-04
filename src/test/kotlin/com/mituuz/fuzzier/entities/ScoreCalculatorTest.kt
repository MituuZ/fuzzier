/*
 *  MIT License
 *
 *  Copyright (c) 2025 Mitja Leino
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */
package com.mituuz.fuzzier.entities

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ScoreCalculatorTest {
    @Test
    fun `Search string contained same index`() {
        val sc = ScoreCalculator("test", MatchConfig(), mapOf())
        sc.searchStringIndex = 0
        sc.searchStringLength = 4

        sc.currentFilePath = "test"
        sc.filePathIndex = 0

        assertTrue(sc.canSearchStringBeContained())
    }

    @Test
    fun `Search string contained different index`() {
        val sc = ScoreCalculator("test", MatchConfig(), mapOf())
        sc.searchStringIndex = 0
        sc.searchStringLength = 4

        sc.currentFilePath = "test"
        sc.filePathIndex = 1

        assertFalse(sc.canSearchStringBeContained())
    }

    @Test
    fun `Basic streak happy case`() {
        val matchConfig = MatchConfig(
            matchWeightStreakModifier = 10
        )
        val sc = ScoreCalculator("test", matchConfig, mapOf())

        val fScore = sc.calculateScore("/test")
        assertEquals(4, fScore!!.streakScore)
    }

    @Test
    fun `Basic streak longer path`() {
        val matchConfig = MatchConfig(
            matchWeightStreakModifier = 10
        )
        val sc = ScoreCalculator("test", matchConfig, mapOf())

        val fScore = sc.calculateScore("/te/st")
        assertEquals(2, fScore!!.streakScore)
    }

    @Test
    fun `Basic streak no possible match`() {
        val matchConfig = MatchConfig(
            matchWeightStreakModifier = 10
        )
        val sc = ScoreCalculator("test", matchConfig, mapOf())

        val fScore = sc.calculateScore("/te")
        assertNull(fScore)
    }

    @Test
    fun `Multi match basic test`() {
        val matchConfig = MatchConfig(
            matchWeightSingleChar = 10, multiMatch = true
        )
        val sc = ScoreCalculator("test", matchConfig, mapOf())

        val fScore = sc.calculateScore("/test")
        assertEquals(4, fScore!!.multiMatchScore)
    }

    @Test
    fun `Multi match basic test multiples`() {
        val matchConfig = MatchConfig(
            matchWeightSingleChar = 10, multiMatch = true
        )
        val sc = ScoreCalculator("test", matchConfig, mapOf())
        val fScore = sc.calculateScore("/testtest")
        assertEquals(8, fScore!!.multiMatchScore)
    }

    @Test
    fun `Multi match basic test multiples multiples`() {
        val matchConfig = MatchConfig(
            matchWeightSingleChar = 10, multiMatch = true
        )
        val sc = ScoreCalculator("test test", matchConfig, mapOf())
        val fScore = sc.calculateScore("/testtest")
        assertEquals(8, fScore!!.multiMatchScore)
    }

    @Test
    fun `Partial path score basic test`() {
        val matchConfig = MatchConfig(
            matchWeightPartialPath = 1
        )
        val sc = ScoreCalculator("test", matchConfig, mapOf())
        val fScore = sc.calculateScore("/test.kt")
        assertEquals(1, fScore!!.partialPathScore)
    }

    @Test
    fun `Filename score basic test`() {
        val matchConfig = MatchConfig(
            matchWeightFilename = 10
        )
        val sc = ScoreCalculator("test", matchConfig, mapOf())
        val fScore = sc.calculateScore("/test.kt")
        assertEquals(4, fScore!!.filenameScore)
    }

    @Test
    fun `Empty ss and fp`() {
        val sc = ScoreCalculator("", MatchConfig(), mapOf())

        val fScore = sc.calculateScore("")
        assertEquals(0, fScore!!.getTotalScore())
    }

    // Legacy tests
    @Test
    fun `Non-consecutive streak`() {
        val matchConfig = MatchConfig(
            matchWeightStreakModifier = 10, multiMatch = true, matchWeightSingleChar = 10, matchWeightFilename = 10
        )
        val sc = ScoreCalculator("kif", matchConfig, mapOf())

        val fScore = sc.calculateScore("/KotlinIsFun")

        assertEquals(1, fScore!!.streakScore)
        assertEquals(4, fScore.multiMatchScore)
        assertEquals(0, fScore.partialPathScore)
        assertEquals(1, fScore.filenameScore)
    }

    @Test
    fun `Consecutive streak`() {
        val matchConfig = MatchConfig(
            matchWeightStreakModifier = 10, multiMatch = true, matchWeightSingleChar = 10, matchWeightFilename = 10
        )
        val sc = ScoreCalculator("kot", matchConfig, mapOf())

        val fScore = sc.calculateScore("/KotlinIsFun")

        assertEquals(3, fScore!!.streakScore)
        assertEquals(3, fScore.multiMatchScore)
        assertEquals(0, fScore.partialPathScore)
        assertEquals(3, fScore.filenameScore)
    }

    @Test
    fun `Too long ss`() {
        val sc = ScoreCalculator("TooLongSearchString", MatchConfig(), mapOf())
        val fScore = sc.calculateScore("/KIF")
        assertNull(fScore)
    }

    @Test
    fun `No possible match`() {
        val sc = ScoreCalculator("A", MatchConfig(), mapOf())
        val fScore = sc.calculateScore("/KIF")
        assertNull(fScore)
    }

    @Test
    fun `Empty ss`() {
        val sc = ScoreCalculator("", MatchConfig(), mapOf())

        val fScore = sc.calculateScore("/KIF")
        assertEquals(0, fScore!!.getTotalScore())
    }

    @Test
    fun `No possible match split`() {
        val sc = ScoreCalculator("A A B", MatchConfig(), mapOf())
        val fScore = sc.calculateScore("/Kotlin/Is/Fun/kif.kt")
        assertNull(fScore)
    }

    @Test
    fun `Partial match split`() {
        val sc = ScoreCalculator("A A K", MatchConfig(), mapOf())
        val fScore = sc.calculateScore("/Kotlin/Is/Fun/kif.kt")
        assertNull(fScore)
    }

    @Test
    fun `Split match for space`() {
        val matchConfig = MatchConfig(
            matchWeightStreakModifier = 10, multiMatch = true, matchWeightSingleChar = 10, matchWeightFilename = 10
        )
        val sc = ScoreCalculator("fun kotlin", matchConfig, mapOf())

        val fScore = sc.calculateScore("/Kotlin/Is/Fun/kif.kt")

        assertEquals(6, fScore!!.streakScore)
        assertEquals(15, fScore.multiMatchScore)
        assertEquals(20, fScore.partialPathScore)
        assertEquals(1, fScore.filenameScore)
    }

    @Test
    fun `Legacy test 1`() {
        val matchConfig = MatchConfig(
            matchWeightStreakModifier = 10, multiMatch = true, matchWeightSingleChar = 10, matchWeightFilename = 10
        )
        val sc = ScoreCalculator("kif", matchConfig, mapOf())

        val fScore = sc.calculateScore("/Kotlin/Is/Fun/kif.kt")

        assertEquals(1, fScore!!.streakScore)
        assertEquals(8, fScore.multiMatchScore)
        assertEquals(10, fScore.partialPathScore)
        assertEquals(3, fScore.filenameScore)
    }

    @Test
    fun `Legacy test 2`() {
        val matchConfig = MatchConfig(
            matchWeightStreakModifier = 10, multiMatch = true, matchWeightSingleChar = 10, matchWeightFilename = 10
        )
        val sc = ScoreCalculator("kif", matchConfig, mapOf())

        val fScore = sc.calculateScore("/Kiffer/Is/Fun/kiffer.kt")

        assertEquals(3, fScore!!.streakScore)
        assertEquals(11, fScore.multiMatchScore)
        assertEquals(0, fScore.partialPathScore)
        assertEquals(3, fScore.filenameScore)
    }

    @Test
    fun `Multiple partial file path matches`() {
        val matchConfig = MatchConfig(
            matchWeightStreakModifier = 10, multiMatch = true, matchWeightSingleChar = 10, matchWeightFilename = 10
        )
        val sc = ScoreCalculator("kif", matchConfig, mapOf())

        val fScore = sc.calculateScore("/Kif/Is/Fun/kif.kt")

        assertEquals(3, fScore!!.streakScore)
        assertEquals(9, fScore.multiMatchScore)
        assertEquals(20, fScore.partialPathScore)
        assertEquals(3, fScore.filenameScore)
    }

    @Test
    fun `Legacy test 3`() {
        val matchConfig = MatchConfig(
            matchWeightStreakModifier = 10, multiMatch = true, matchWeightSingleChar = 10, matchWeightFilename = 10
        )
        val sc = ScoreCalculator("kif fun kotlin", matchConfig, mapOf())

        val fScore = sc.calculateScore("/Kotlin/Is/Fun/kif.kt")

        assertEquals(6, fScore!!.streakScore)
        assertEquals(15, fScore.multiMatchScore)
        assertEquals(30, fScore.partialPathScore)
        assertEquals(3, fScore.filenameScore)
    }

    @Test
    fun `5 tolerance matching normal match`() {
        val matchConfig = MatchConfig(
            tolerance = 5,
        )
        val sc = ScoreCalculator("kotlin", matchConfig, mapOf())

        assertNotNull(sc.calculateScore("/Kotlin"))
    }

    @Test
    fun `5 tolerance matching 1 letter difference`() {
        val matchConfig = MatchConfig(
            tolerance = 5,
        )
        val sc = ScoreCalculator("korlin", matchConfig, mapOf())

        assertNotNull(sc.calculateScore("/Kotlin"))
    }

    @Test
    fun `1 tolerance matching 1 letter difference`() {
        val matchConfig = MatchConfig(
            tolerance = 1,
        )
        val sc = ScoreCalculator("korlin", matchConfig, mapOf())

        assertNotNull(sc.calculateScore("/Kotlin"))
    }

    @Test
    fun `1 tolerance matching 2 letter difference`() {
        val matchConfig = MatchConfig(
            tolerance = 1,
        )
        val sc = ScoreCalculator("korlnn", matchConfig, mapOf())

        assertNull(sc.calculateScore("/Kotlin"))
    }

    @Test
    fun `1 tolerance matching 1 letter difference with split path`() {
        val matchConfig = MatchConfig(
            tolerance = 1,
        )
        val sc = ScoreCalculator("korlin", matchConfig, mapOf())

        assertNotNull(sc.calculateScore("/Kot/lin"))
    }

    @Test
    fun `1 tolerance matching 2 letter difference with split path`() {
        val matchConfig = MatchConfig(
            tolerance = 1,
        )
        val sc = ScoreCalculator("korlin", matchConfig, mapOf())

        assertNull(sc.calculateScore("/Kot/sin"))
    }

    @Test
    fun `2 tolerance matching 2 letter difference with split path`() {
        val matchConfig = MatchConfig(
            tolerance = 2,
        )
        val sc = ScoreCalculator("korlin", matchConfig, mapOf())

        assertNotNull(sc.calculateScore("/Kot/sin"))
    }

    @Test
    fun `Don't match longer strings even if there is tolerance left`() {
        val matchConfig = MatchConfig(
            tolerance = 5,
        )
        val sc = ScoreCalculator("kotlin12345", matchConfig, mapOf())

        assertNull(sc.calculateScore("/Kotlin"))
    }


    @Test
    fun `Usage boost with null stats`() {
        val sc = ScoreCalculator("", MatchConfig(), mapOf())
        assertEquals(0, sc.calculateUsageBoost(null))
    }

    @Test
    fun `Usage boost with recent index 0`() {
        val sc = ScoreCalculator("", MatchConfig(), mapOf())
        assertEquals(10, sc.calculateUsageBoost(FileUsageStats(0, 0)))
    }

    @Test
    fun `Usage boost with recent index 10`() {
        val sc = ScoreCalculator("", MatchConfig(), mapOf())
        assertEquals(5, sc.calculateUsageBoost(FileUsageStats(10, 0)))
    }

    @Test
    fun `Usage boost with recent index 20`() {
        val sc = ScoreCalculator("", MatchConfig(), mapOf())
        assertEquals(0, sc.calculateUsageBoost(FileUsageStats(20, 0)))
    }

    @Test
    fun `Usage boost with access count`() {
        val sc = ScoreCalculator("", MatchConfig(), mapOf())
        assertEquals(11, sc.calculateUsageBoost(FileUsageStats(0, 5)))
    }

    @Test
    fun `Usage boost affects total score`() {
        val fileUsageStats = mapOf("/test.kt" to FileUsageStats(0, 5))
        val sc = ScoreCalculator("test", MatchConfig(), fileUsageStats)
        val fScore = sc.calculateScore("/test.kt")
        assertNotNull(fScore)
        assertEquals(1, fScore!!.frequencyScore)
        assertEquals(10, fScore.recencyScore)
        // streakScore = (4 * 10) / 10 = 4
        // filenameScore = (4 * 20) / 10 = 8
        // partialPathScore = 10 (default weight)
        // total = 1 + 10 + 4 + 8 + 10 = 33
        assertEquals(33, fScore.getTotalScore())
    }

    @Test
    fun `Usage boost affects total score with weights`() {
        val fileUsageStats = mapOf("/test.kt" to FileUsageStats(0, 5))
        val matchConfig = MatchConfig(matchWeightFrequency = 20, matchWeightRecency = 5)
        val sc = ScoreCalculator("test", matchConfig, fileUsageStats)
        val fScore = sc.calculateScore("/test.kt")
        assertNotNull(fScore)
        // Frequency boost: (1 * 20) / 10 = 2
        // Recency boost: (10 * 5) / 10 = 5
        assertEquals(2, fScore!!.frequencyScore)
        assertEquals(5, fScore.recencyScore)
        // streakScore = (4 * 10) / 10 = 4
        // filenameScore = (4 * 20) / 10 = 8
        // partialPathScore = 10 (default weight)
        // total = 2 + 5 + 4 + 8 + 10 = 29
        assertEquals(29, fScore.getTotalScore())
    }

    @Test
    fun `Recency boost is smooth`() {
        val sc = ScoreCalculator("", MatchConfig(), mapOf())
        assertEquals(10, sc.calculateUsageBoost(FileUsageStats(0, 0)))
        assertEquals(9, sc.calculateUsageBoost(FileUsageStats(2, 0)))
        assertEquals(8, sc.calculateUsageBoost(FileUsageStats(4, 0)))
        assertEquals(5, sc.calculateUsageBoost(FileUsageStats(10, 0)))
        assertEquals(0, sc.calculateUsageBoost(FileUsageStats(20, 0)))
        assertEquals(0, sc.calculateUsageBoost(FileUsageStats(30, 0)))
    }

    @Test
    fun `Not in recent list is not better than late in recent list`() {
        val sc = ScoreCalculator("", MatchConfig(), mapOf())
        
        val notInListBoost = sc.calculateUsageBoost(null) // 0
        val lateInListBoost = sc.calculateUsageBoost(FileUsageStats(20, 0)) // 0
        
        assertEquals(notInListBoost, lateInListBoost, "File NOT in list should be equal to a file that is very old in the list")
    }

    @Test
    fun `Frequency boost is capped`() {
        val sc = ScoreCalculator("", MatchConfig(), mapOf())
        // 100 accesses gives 10 points (capped)
        // index 10 gives 5 points
        assertEquals(15, sc.calculateUsageBoost(FileUsageStats(10, 100)))
    }

    @Test
    fun `Usage boost with weights set to 0`() {
        val matchConfig = MatchConfig(matchWeightFrequency = 0, matchWeightRecency = 0)
        val sc = ScoreCalculator("", matchConfig, mapOf())
        assertEquals(0, sc.calculateUsageBoost(FileUsageStats(0, 100)))
    }

    @Test
    fun `Multi match weight affects total score`() {
        val matchConfig = MatchConfig(
            multiMatch = true,
            matchWeightSingleChar = 20
        )
        val sc = ScoreCalculator("test", matchConfig, mapOf())
        val fScore = sc.calculateScore("/test.kt")
        assertNotNull(fScore)

        // "test.kt" contains 't', 'e', 's', 't', 't' -> 5 matches.
        // weight 20. (5 * 20) / 10 = 10.
        assertEquals(10, fScore!!.multiMatchScore)
    }

    @Test
    fun `All weights affect total score`() {
        val fileUsageStats = mapOf("/test.kt" to FileUsageStats(0, 5))
        val matchConfig = MatchConfig(
            matchWeightStreakModifier = 20,
            matchWeightFilename = 30,
            matchWeightPartialPath = 5,
            matchWeightFrequency = 20,
            matchWeightRecency = 5
        )
        val sc = ScoreCalculator("test", matchConfig, fileUsageStats)
        val fScore = sc.calculateScore("/test.kt")
        assertNotNull(fScore)

        // streakScore: longestStreak=4, weight=20. (4 * 20) / 10 = 8
        // filenameScore: longestFilenameStreak=4, weight=30. (4 * 30) / 10 = 12
        // partialPathScore: weight=5. 5
        // frequencyScore: access=5, weight=20. (1 * 20) / 10 = 2
        // recencyScore: index=0, weight=5. (10 * 5) / 10 = 5

        assertEquals(8, fScore!!.streakScore)
        assertEquals(12, fScore.filenameScore)
        assertEquals(5, fScore.partialPathScore)
        assertEquals(2, fScore.frequencyScore)
        assertEquals(5, fScore.recencyScore)
        assertEquals(8 + 12 + 5 + 2 + 5, fScore.getTotalScore()) // 32
    }
}