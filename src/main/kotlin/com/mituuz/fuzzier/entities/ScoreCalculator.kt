package com.mituuz.fuzzier.entities

import com.intellij.openapi.components.service
import com.mituuz.fuzzier.settings.FuzzierSettingsService

class ScoreCalculator(private val searchString: String) {
    // TODO: We can parse/handle the search string here, just once per search
    private var currentIndex: Int = 0
    private var longestStreak: Int = 0

    // Set up the settings
    private val settings = service<FuzzierSettingsService>().state
    private val matchWeightSingleChar = settings.matchWeightSingleChar
    private val matchWeightStreakModifier = settings.matchWeightStreakModifier
    private val matchWeightPartialPath = settings.matchWeightPartialPath

    /**
     * Returns null if no match can be found
     */
    fun calculateScore(filePath: String, filename: String): Int? {
        currentIndex = 0
        longestStreak = 0

        // Check if the search string is longer than the file path, which results in no match
        if (searchString.length > filePath.length) { // TODO: + tolerance when it is implemented
            return null
        }

        return 0
    }
}