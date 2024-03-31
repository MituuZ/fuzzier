package com.mituuz.fuzzier.entities

import com.intellij.openapi.components.service
import com.mituuz.fuzzier.settings.FuzzierSettingsService

class ScoreCalculator(private val searchString: String) {
    // TODO: We can parse/handle the search string here, just once per search
    private val searchStringParts = searchString.split(" ")

    private var searchStringIndex: Int = 0
    private var searchStringLength: Int = 0
    private var filePathIndex: Int = 0

    // Set up the settings
    private val settings = service<FuzzierSettingsService>().state
    private val matchWeightSingleChar = settings.matchWeightSingleChar
    private val matchWeightStreakModifier = settings.matchWeightStreakModifier
    private val matchWeightPartialPath = settings.matchWeightPartialPath

    private var currentFilePath = ""
    private var longestStreak: Int = 0

    /**
     * Returns null if no match can be found
     */
    fun calculateScore(filePath: String, filename: String): Int? { // TODO: This should return FuzzyScore
        this.currentFilePath = filePath
        longestStreak = 0

        // Check if the search string is longer than the file path, which results in no match
        if (searchString.length > currentFilePath.length) { // TODO: + tolerance when it is implemented
            return null
        }

        for (part in searchStringParts) {
            // Reset the index and length for the new part
            searchStringIndex = 0
            searchStringLength = part.length
            filePathIndex = 0

            if (!processString(part)) {
                return null
            }
        }

        return 0
    }

    /**
     * Returns false if no match can be found, this stops the search
     */
    private fun processString(searchStringPart: String): Boolean {
        var searchStringIndex = 0;
        while (searchStringIndex < searchStringLength) {
            if (!canSearchStringBeContained()) {
                return false
            }

            if (!processChar()) {
                return false
            }
            searchStringIndex++
        }

        return true
    }

    private fun processChar(): Boolean {
        return true
    }

    /**
     * Checks if the remaining search string can be contained in the remaining file path
     * e.g. if the remaining search string is "abc" and the remaining file path is "abcdef", it can be contained
     * e.g. if the remaining search string is "abc" and the remaining file path is "def", it can't be contained
     * e.g. if the remaining search string is "abc" and the remaining file path is "ab", it can't be contained
     */
    private fun canSearchStringBeContained(): Boolean {
        val remainingSearchStringLength = searchStringLength - searchStringIndex
        val remainingFilePathLength = currentFilePath.length - filePathIndex
        return remainingSearchStringLength <= remainingFilePathLength // TODO: + tolerance when it is implemented
    }
}