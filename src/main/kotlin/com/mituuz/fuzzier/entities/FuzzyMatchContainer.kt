package com.mituuz.fuzzier.entities

import com.intellij.openapi.components.service
import com.mituuz.fuzzier.settings.FuzzierSettingsService

class FuzzyMatchContainer(val score: FuzzyScore, var filePath: String, var filename: String, private var module: String = "") {
    fun toString(filenameType: FilenameType): String {
        return when (filenameType) {
            FilenameType.FILENAME_ONLY -> filename
            FilenameType.FILE_PATH_ONLY -> filePath
            FilenameType.FILENAME_WITH_PATH -> "$filename   ($filePath)"
            FilenameType.FILENAME_WITH_PATH_STYLED -> getFilenameWithPathStyled()
        }
    }

    private fun getFilenameWithPathStyled(): String {
        return "<html><strong>$filename</strong>  <i>($filePath)</i></html>"
    }

    fun getFileUri(): String {
        val basePath = service<FuzzierSettingsService>().state.modules[module]
        return "$basePath$filePath"
    }

    fun getScore(): Int {
        return score.getTotalScore()
    }

    /**
     * Gets score that is prioritizing shorter dir paths
     */
    fun getScoreWithDirLength(): Int {
        return score.getTotalScore() + filePath.length * -1
    }

    enum class FilenameType(val text: String) {
        FILE_PATH_ONLY("File path only"),
        FILENAME_ONLY("Filename only"),
        FILENAME_WITH_PATH("Filename with (path)"),
        FILENAME_WITH_PATH_STYLED("Filename with (path) styled")
    }

    class FuzzyScore {
        var streakScore = 0
        var multiMatchScore = 0
        var partialPathScore = 0
        var filenameScore = 0

        fun getTotalScore(): Int {
            return streakScore + multiMatchScore + partialPathScore + filenameScore
        }
    }
}