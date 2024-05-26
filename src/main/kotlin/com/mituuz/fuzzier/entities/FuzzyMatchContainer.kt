/*
MIT License

Copyright (c) 2024 Mitja Leino

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
        if (basePath != null) {
            return "$basePath$filePath"
        }
        return filePath
    }

    fun getScore(): Int {
        return score.getTotalScore()
    }

    /**
     * Gets score that is prioritizing shorter dir paths
     */
    fun getScoreWithDirLength(): Int {
        return score.getTotalScore() + filePath.length * -5
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