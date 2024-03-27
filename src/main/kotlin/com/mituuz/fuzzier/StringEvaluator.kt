package com.mituuz.fuzzier

import com.intellij.openapi.roots.ContentIterator
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.openapi.vfs.VirtualFile
import org.apache.commons.lang3.StringUtils
import javax.swing.DefaultListModel

class StringEvaluator(
    private var multiMatch: Boolean, private var exclusionList: Set<String>, private var matchWeightSingleChar: Int,
    private var matchWeightStreakModifier: Int, private var matchWeightPartialPath: Int, private var changeListManager: ChangeListManager? = null) {
    fun getContentIterator(projectBasePath: String, searchString: String, listModel: DefaultListModel<FuzzyMatchContainer>): ContentIterator {
        return ContentIterator { file: VirtualFile ->
            if (!file.isDirectory) {
                val filePath = projectBasePath.let { it1 -> file.path.removePrefix(it1) }
                if (isExcluded(file, filePath)) {
                    return@ContentIterator true
                }
                if (filePath.isNotBlank()) {
                    val fuzzyMatchContainer = fuzzyContainsCaseInsensitive(filePath, searchString)
                    if (fuzzyMatchContainer != null) {
                        listModel.addElement(fuzzyMatchContainer)
                    }
                }
            }
            true
        }
    }

    private fun isExcluded(file: VirtualFile, filePath: String): Boolean {
        if (changeListManager !== null) {
            return changeListManager!!.isIgnoredFile(file)
        }
        for (e in exclusionList) {
            when {
                e.startsWith("*") -> {
                    if (filePath.endsWith(e.substring(1))) {
                        return true
                    }
                }
                e.endsWith("*") -> {
                    if (filePath.startsWith(e.substring(0, e.length - 1))) {
                        return true
                    }
                }
                filePath.contains(e) -> {
                    return true
                }
            }
        }
        return false
    }

    fun fuzzyContainsCaseInsensitive(filePath: String, searchString: String): FuzzyMatchContainer? {
        if (searchString.isBlank()) {
            val filename = filePath.substring(filePath.lastIndexOf("/") + 1)
            return FuzzyMatchContainer(0, filePath, filename)
        }
        if (searchString.length > filePath.length) {
            return null
        }

        val lowerFilePath: String = filePath.lowercase()
        val lowerSearchString: String = searchString.lowercase()
        return getFuzzyMatch(lowerFilePath, lowerSearchString, filePath)
    }

    private fun getFuzzyMatch(lowerFilePath: String, lowerSearchString: String, filePath: String): FuzzyMatchContainer? {
        var score = 0
        for (s in StringUtils.split(lowerSearchString, " ")) {
            score += processSearchString(s, lowerFilePath) ?: return null
        }
        val filename = filePath.substring(filePath.lastIndexOf("/") + 1)
        return FuzzyMatchContainer(score, filePath, filename)
    }

    private fun processSearchString(s: String, lowerFilePath: String): Int? {
        var longestStreak = 0
        var streak = 0
        var score = 0.0
        var prevIndex = -10
        var match = 0
        for (searchStringIndex in s.indices) {
            if (lowerFilePath.length - searchStringIndex < s.length - searchStringIndex) {
                return null
            }

            var found = -1
            // Always process the whole file path for each character, assuming they're found
            for (filePathIndex in lowerFilePath.indices) {
                if (s[searchStringIndex] == lowerFilePath[filePathIndex]) {
                    match++
                    // Always increase score when finding a match
                    if (multiMatch) {
                        score += matchWeightSingleChar / 10.0
                    }
                    // Only check streak and update the found variable, if the current match index is greater than the previous
                    if (found == -1 && filePathIndex > prevIndex) {
                        // TODO: Does not work quite correct when handling a search string where a char is found first and then again for a multi match
                        // If the index is one greater than the previous chars, increment streak and update the longest streak
                        if (prevIndex + 1 == filePathIndex) {
                            streak++
                            if (streak > longestStreak) {
                                longestStreak = streak
                            }
                        } else {
                            streak = 1
                        }
                        // Save the first found index of a new character
                        prevIndex = filePathIndex
                        if (!multiMatch) {
                            // Set found to verify a match and exit the loop
                            found = filePathIndex
                            continue;
                        }
                    }
                    // When multiMatch is disabled, setting found exits the loop. Only set found for multiMatch
                    if (multiMatch) {
                        found = filePathIndex
                    }
                }
            }

            // Check that the character was found and that it was found after the previous characters index
            // Here we could skip once to broaden the search
            if (found == -1 || prevIndex > found) {
                return null
            }
        }

        // If we get to here, all characters were found and have been accounted for in the score
        return calculateScore(streak, longestStreak, lowerFilePath, s, score)
    }

    private fun calculateScore(streak: Int, longestStreak: Int, lowerFilePath: String, lowerSearchString: String, stringComparisonScore: Double): Int {
        var score: Double = if (streak > longestStreak) {
            (matchWeightStreakModifier / 10.0) * streak + stringComparisonScore
        } else {
            (matchWeightStreakModifier / 10.0) * longestStreak + stringComparisonScore
        }

        StringUtils.split(lowerFilePath, "/.").forEach {
            if (it == lowerSearchString) {
                score += matchWeightPartialPath
            }
        }

        return score.toInt()
    }

    data class FuzzyMatchContainer(val score: Int, val filePath: String, val filename: String) {
        fun toString(filenameType: FilenameType): String {
            return when (filenameType) {
                FilenameType.FILENAME_ONLY -> filename
                FilenameType.FILEPATH_ONLY -> filePath
                FilenameType.FILENAME_WITH_PATH -> "$filename   ($filePath)"
            }
        }
    }

    enum class FilenameType(val text: String) {
        FILEPATH_ONLY("Filepath only"),
        FILENAME_ONLY("Filename only"),
        FILENAME_WITH_PATH("Filename with (path)")
    }

    fun setSettings(multiMatch: Boolean, matchWeightSingleChar: Int, matchWeightPartialPath: Int,
                    matchWeightStreakModifier: Int) {
        this.multiMatch = multiMatch
        this.matchWeightPartialPath = matchWeightPartialPath
        this.matchWeightSingleChar = matchWeightSingleChar
        this.matchWeightStreakModifier = matchWeightStreakModifier
    }
}