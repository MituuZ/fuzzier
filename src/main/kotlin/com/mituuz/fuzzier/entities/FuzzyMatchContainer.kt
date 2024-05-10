package com.mituuz.fuzzier.entities

import com.intellij.openapi.components.service
import com.mituuz.fuzzier.settings.FuzzierSettingsService

class FuzzyMatchContainer(val score: FuzzyScore, var filePath: String, var filename: String, private var module: String = "") {
    fun toString(filenameType: FilenameType, includeModule: Boolean): String {
        var addInfo = ""
        if (includeModule) {
            addInfo = module
        }
        return when (filenameType) {
            FilenameType.FILENAME_ONLY -> filename
            FilenameType.FILE_PATH_ONLY -> addInfo + filePath
            FilenameType.FILENAME_WITH_PATH -> "$filename   ($addInfo$filePath)"
            FilenameType.FILENAME_WITH_PATH_STYLED -> getFilenameWithPathStyled(addInfo)
        }
    }

    private fun getFilenameWithPathStyled(addInfo: String): String {
        return "<html><strong>$filename</strong>  <i>($addInfo$filePath)</i></html>"
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