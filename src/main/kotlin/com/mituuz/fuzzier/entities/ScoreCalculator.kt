package com.mituuz.fuzzier.entities

import com.intellij.openapi.components.service
import com.mituuz.fuzzier.entities.FuzzyMatchContainer.FuzzyScore
import com.mituuz.fuzzier.settings.FuzzierSettingsService
import org.apache.commons.lang3.StringUtils

class ScoreCalculator(searchString: String) {
    private val lowerSearchString: String = searchString.lowercase()
    private val searchStringParts = lowerSearchString.split(" ")
    private lateinit var fuzzyScore: FuzzyScore
    private val uniqueLetters = lowerSearchString.toSet()
    private lateinit var lowerFilePath: String

    var searchStringIndex: Int = 0
    var searchStringLength: Int = 0
    var filePathIndex: Int = 0
    private var filenameIndex: Int = 0

    // Set up the settings
    private val settings = service<FuzzierSettingsService>().state
    private var multiMatch = settings.multiMatch
    private var matchWeightSingleChar = settings.matchWeightSingleChar
    private var matchWeightStreakModifier = settings.matchWeightStreakModifier
    private var matchWeightPartialPath = settings.matchWeightPartialPath
    private var matchWeightFilename = 1

    var currentFilePath = ""
    private var longestStreak: Int = 0
    private var currentStreak: Int = 0
    private var longestFilenameStreak: Int = 0
    private var currentFilenameStreak: Int = 0

    /**
     * Returns null if no match can be found
     */
    fun calculateScore(filePath: String): FuzzyScore? { // TODO: This should return FuzzyScore
        lowerFilePath = filePath.lowercase()
        filenameIndex = lowerFilePath.lastIndexOf("/") + 1
        currentFilePath = lowerFilePath
        longestStreak = 0
        fuzzyScore = FuzzyScore()

        // Check if the search string is longer than the file path, which results in no match
        if (lowerSearchString.length > currentFilePath.length) { // TODO: + tolerance when it is implemented
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

        fuzzyScore.streakScore = (longestStreak * matchWeightStreakModifier) / 10
        fuzzyScore.filenameScore = longestFilenameStreak * matchWeightFilename

        return fuzzyScore
    }

    /**
     * Returns false if no match can be found, this stops the search
     */
    private fun processString(searchStringPart: String): Boolean {
        if (multiMatch) {
            calculateMultiMatchScore()
        }

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
        val filePathPartChar = currentFilePath[filePathIndex]
        if (searchStringPartChar == filePathPartChar) {
            searchStringIndex++
            updateStreak(true)
            // Increase the score
        } else {
            updateStreak(false)
            // Decrease the score
        }
        filePathIndex++
        return true
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
        if (filePathIndex >= filenameIndex) {
            if (match) {
                currentFilenameStreak++
                if (currentFilenameStreak > longestFilenameStreak) {
                    longestFilenameStreak = currentFilenameStreak
                }
            } else {
                currentFilenameStreak = 0
            }
        }
    }

    /**
     * Checks if the remaining search string can be contained in the remaining file path
     * e.g. if the remaining search string is "abc" and the remaining file path is "abcdef", it can be contained
     * e.g. if the remaining search string is "abc" and the remaining file path is "def", it can't be contained
     * e.g. if the remaining search string is "abc" and the remaining file path is "ab", it can't be contained
     */
    fun canSearchStringBeContained(): Boolean {
        // TODO: Can this be handled in another way?
        val remainingSearchStringLength = searchStringLength - searchStringIndex
        val remainingFilePathLength = currentFilePath.length - filePathIndex
        return remainingSearchStringLength <= remainingFilePathLength // TODO: + tolerance when it is implemented
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
}