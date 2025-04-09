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

import com.intellij.openapi.components.service
import com.mituuz.fuzzier.entities.FuzzyMatchContainer.FuzzyScore
import com.mituuz.fuzzier.settings.FuzzierGlobalSettingsService
import org.apache.commons.lang3.StringUtils

class ScoreCalculator(searchString: String) {
    private val lowerSearchString: String = searchString.lowercase()
    private val searchStringParts = lowerSearchString.split(" ")
    private lateinit var fuzzyScore: FuzzyScore
    private val uniqueLetters = lowerSearchString.toSet()

    var searchStringIndex: Int = 0
    var searchStringLength: Int = 0
    var filePathIndex: Int = 0
    private var filenameIndex: Int = 0

    // Set up match settings
    private val globalState = service<FuzzierGlobalSettingsService>().state
    private var tolerance = globalState.tolerance
    private var multiMatch = globalState.multiMatch
    private var matchWeightSingleChar = globalState.matchWeightSingleChar
    private var matchWeightStreakModifier = globalState.matchWeightStreakModifier
    private var matchWeightPartialPath = globalState.matchWeightPartialPath
    private var matchWeightFilename = globalState.matchWeightFilename

    var currentFilePath = ""
    private var longestStreak: Int = 0
    private var currentStreak: Int = 0
    private var longestFilenameStreak: Int = 0
    private var currentFilenameStreak: Int = 0
    private var toleranceCount: Int = 0

    /**
     * Returns null if no match can be found
     */
    fun calculateScore(filePath: String): FuzzyScore? {
        currentFilePath = filePath.lowercase()
        filenameIndex = currentFilePath.lastIndexOf("/") + 1
        longestStreak = 0
        fuzzyScore = FuzzyScore()
        toleranceCount = 0

        // Check if the search string is longer than the file path, which results in no match
        if (lowerSearchString.length > (currentFilePath.length + tolerance)) {
            return null
        }

        for (part in searchStringParts) {
            // Reset the index and length for the new part
            searchStringIndex = 0
            searchStringLength = part.length
            filePathIndex = 0
            currentStreak = 0

            if (!processString(part)) {
                return null
            }
        }

        if (multiMatch) {
            calculateMultiMatchScore()
        }
        calculateFilenameScore()

        fuzzyScore.streakScore = (longestStreak * matchWeightStreakModifier) / 10
        fuzzyScore.filenameScore = (longestFilenameStreak * matchWeightFilename) / 10

        return fuzzyScore
    }

    /**
     * Returns false if no match can be found, this stops the search
     */
    private fun processString(searchStringPart: String): Boolean {
        while (searchStringIndex < searchStringLength) {
            if (!canSearchStringBeContained()) {
                return false
            }

            val currentChar = searchStringPart[searchStringIndex]
            if (!processChar(currentChar)) {
                return false
            }
        }

        calculatePartialPathScore(searchStringPart)

        return true
    }

    private fun calculateMultiMatchScore() {
        fuzzyScore.multiMatchScore += (currentFilePath.count { it in uniqueLetters } * matchWeightSingleChar) / 10
    }

    private fun calculatePartialPathScore(searchStringPart: String) {
        StringUtils.split(currentFilePath, "/.").forEach {
            if (it == searchStringPart) {
                fuzzyScore.partialPathScore += matchWeightPartialPath
            }
        }
    }

    private fun processChar(searchStringPartChar: Char): Boolean {
        if (filePathIndex >= currentFilePath.length) {
            return false
        }
        val filePathPartChar = currentFilePath[filePathIndex]
        if (searchStringPartChar == filePathPartChar) {
            searchStringIndex++
            updateStreak(true)
        } else {
            if (currentStreak > 0 && toleranceCount < tolerance) {
                // When hitting tolerance increment the search string and filepath, but do not add streak
                searchStringIndex++
                toleranceCount++
                filePathIndex++
                return true
            }
            updateStreak(false)
        }
        filePathIndex++
        return true
    }

    /**
     * Go through the search string one more time and calculate the longest streak present in the filename
     */
    private fun calculateFilenameScore() {
        searchStringIndex = 0
        currentFilenameStreak = 0
        longestFilenameStreak = 0

        filePathIndex = filenameIndex
        while (searchStringIndex < searchStringLength && filePathIndex < currentFilePath.length) {
            processFilenameChar(lowerSearchString[searchStringIndex])
        }
    }

    private fun processFilenameChar(searchStringPartChar: Char) {
        val filePathPartChar = currentFilePath[filePathIndex]
        if (searchStringPartChar == filePathPartChar) {
            fuzzyScore.highlightCharacters.add(filePathIndex - filenameIndex)
            searchStringIndex++
            currentFilenameStreak++
            if (currentFilenameStreak > longestFilenameStreak) {
                longestFilenameStreak = currentFilenameStreak
            }
        } else {
            currentFilenameStreak = 0
        }
        filePathIndex++
    }

    private fun updateStreak(match: Boolean) {
        if (match) {
            currentStreak++
            if (currentStreak > longestStreak) {
                longestStreak = currentStreak
            }
        } else {
            currentStreak = 0
        }
    }

    /**
     * Checks if the remaining search string can be contained in the remaining file path based on the length
     * e.g. if the remaining search string is "abc" and the remaining file path is "abcdef", it can be contained
     * e.g. if the remaining search string is "abc" and the remaining file path is "def", it can't be contained
     * e.g. if the remaining search string is "abc" and the remaining file path is "ab", it can't be contained
     */
    fun canSearchStringBeContained(): Boolean {
        val remainingSearchStringLength = searchStringLength - searchStringIndex
        val remainingFilePathLength = currentFilePath.length - filePathIndex
        return remainingSearchStringLength <= (remainingFilePathLength + tolerance)
    }

    fun setMatchWeightStreakModifier(value: Int) {
        matchWeightStreakModifier = value
    }

    fun setMatchWeightSingleChar(value: Int) {
        matchWeightSingleChar = value
    }

    fun setMatchWeightPartialPath(value: Int) {
        matchWeightPartialPath = value
    }

    fun setFilenameMatchWeight(value: Int) {
        matchWeightFilename = value
    }

    fun setMultiMatch(value: Boolean) {
        multiMatch = value
    }

    fun setTolerance(value: Int) {
        tolerance = value
    }
}