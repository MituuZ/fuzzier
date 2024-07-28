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
import com.intellij.util.xmlb.Converter
import com.intellij.util.xmlb.XmlSerializationException
import com.mituuz.fuzzier.settings.FuzzierConfiguration.END_STYLE_TAG
import com.mituuz.fuzzier.settings.FuzzierConfiguration.startStyleTag
import com.mituuz.fuzzier.settings.FuzzierSettingsService
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.util.Base64
import javax.swing.DefaultListModel

class FuzzyMatchContainer(
    val score: FuzzyScore,
    var filePath: String,
    var filename: String,
    private var module: String = ""
) : Serializable {
    @Transient
    private var initialPath: String? = null
    private var displayString: String = ""

    companion object {
        /**
         * Used for showing recent files
         *
         * Creates a fuzzy match container with explicitly specified score.
         */
        fun createOrderedContainer(
            order: Int,
            filePath: String,
            initialPath: String,
            filename: String
        ): FuzzyMatchContainer {
            val fuzzyScore = FuzzyScore()
            fuzzyScore.filenameScore = order
            val fuzzyMatchContainer = FuzzyMatchContainer(fuzzyScore, filePath, filename)
            fuzzyMatchContainer.initialPath = initialPath
            return fuzzyMatchContainer
        }
    }

    fun toString(filenameType: FilenameType, highlight: Boolean): String {
        return when (filenameType) {
            FilenameType.FILENAME_ONLY -> filename
            FilenameType.FILE_PATH_ONLY -> filePath
            FilenameType.FILENAME_WITH_PATH -> "$filename   ($filePath)"
            FilenameType.FILENAME_WITH_PATH_STYLED -> getFilenameWithPathStyled(highlight)
        }
    }

    private fun getStyledFilename(highlight: Boolean): String {
        if (highlight) {
            return highlight(filename)
        }
        return filename
    }

    fun highlight(source: String): String {
        val stringBuilder: StringBuilder = StringBuilder(source)
        var offset = 0
        val hlIndexes = score.highlightCharacters.sorted()
        for (i in hlIndexes) {
            if (i < source.length) {
                stringBuilder.insert(i + offset, startStyleTag)
                offset += startStyleTag.length
                stringBuilder.insert(i + offset + 1, END_STYLE_TAG)
                offset += END_STYLE_TAG.length
            }
        }
        return stringBuilder.toString()
    }

    private fun getFilenameWithPathStyled(highlight: Boolean): String {
        return "<html><strong>${getStyledFilename(highlight)}</strong>  <i>($filePath)</i></html>"
    }

    fun getFileUri(): String {
        val basePath = service<FuzzierSettingsService>().state.modules[module]
        if (basePath != null) {
            return "$basePath$filePath"
        }
        if (initialPath != null && initialPath != "") {
            return "$initialPath$filePath"
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

    class FuzzyScore : Serializable {
        var streakScore = 0
        var multiMatchScore = 0
        var partialPathScore = 0
        var filenameScore = 0
        val highlightCharacters: MutableSet<Int> = HashSet()

        fun getTotalScore(): Int {
            return streakScore + multiMatchScore + partialPathScore + filenameScore
        }
    }

    override fun toString(): String {
        return "FuzzyMatchContainer: $filename, score: ${getScore()}, dir score: ${getScoreWithDirLength()}"
    }

    /**
     * This is necessary to persists recently used files between IDE restarts
     *
     * Uses a base 64 encoded string
     *
     * ```
     * @OptionTag(converter = FuzzyMatchContainer.FuzzyMatchContainerConverter::class)
     * var recentlySearchedFiles: DefaultListModel<FuzzyMatchContainer>? = DefaultListModel()
     * ```
     *
     * @see FuzzierSettingsService
     */
    class FuzzyMatchContainerConverter : Converter<DefaultListModel<FuzzyMatchContainer>>() {
        override fun fromString(value: String) : DefaultListModel<FuzzyMatchContainer> {
            // Fallback to an empty list if deserialization fails
            try {
                val data = Base64.getDecoder().decode(value)
                val byteArrayInputStream = ByteArrayInputStream(data)
                return ObjectInputStream(byteArrayInputStream).use { it.readObject() as DefaultListModel<FuzzyMatchContainer> }
            } catch (_: XmlSerializationException) {
                return DefaultListModel<FuzzyMatchContainer>();
            } catch (_: IllegalArgumentException) {
                return DefaultListModel<FuzzyMatchContainer>();
            }
        }

        override fun toString(value: DefaultListModel<FuzzyMatchContainer>) : String {
            val byteArrayOutputStream = ByteArrayOutputStream()
            ObjectOutputStream(byteArrayOutputStream).use { it.writeObject(value) }
            return Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray())
        }
    }
}