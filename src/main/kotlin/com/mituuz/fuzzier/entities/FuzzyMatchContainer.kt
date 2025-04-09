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

import com.intellij.util.xmlb.Converter
import com.mituuz.fuzzier.settings.FuzzierConfiguration.END_STYLE_TAG
import com.mituuz.fuzzier.settings.FuzzierConfiguration.startStyleTag
import com.mituuz.fuzzier.settings.FuzzierGlobalSettingsService
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
    filePath: String,
    filename: String,
    moduleBasePath: String
) : FuzzyContainer(filePath, moduleBasePath, filename) {
    override fun getDisplayString(state: FuzzierGlobalSettingsService.State): String {
        return when (state.filenameType) {
            FilenameType.FILENAME_ONLY -> filename
            FilenameType.FILE_PATH_ONLY -> filePath
            FilenameType.FILENAME_WITH_PATH -> "$filename   ($filePath)"
            FilenameType.FILENAME_WITH_PATH_STYLED -> getFilenameWithPathStyled(state.highlightFilename)
            FilenameType.DEBUG -> toString()
        }
    }

    private fun getFilenameWithPathStyled(highlight: Boolean): String {
        return "<html><strong>${getStyledFilename(highlight)}</strong>  <i>($filePath)</i></html>"
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
        var tagIsOpen = false

        var i = 0
        while (i < hlIndexes.size) {
            val highlightIndex = hlIndexes[i]

            if (!tagIsOpen) {
                stringBuilder.insert(highlightIndex + offset, startStyleTag)
                offset += startStyleTag.length
                tagIsOpen = true
            }

            if (highlightIndex < source.length) {
                if (i + 1 < hlIndexes.size  && hlIndexes[i + 1] == highlightIndex + 1) {
                    i++
                    continue
                }

                stringBuilder.insert(highlightIndex + offset + 1, END_STYLE_TAG)
                offset += END_STYLE_TAG.length
                tagIsOpen = false
            }
            i++
        }

        return stringBuilder.toString()
    }

    fun highlightLegacy(source: String): String {
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

    fun getScore(): Int {
        return score.getTotalScore()
    }

    /**
     * Gets score that is prioritizing shorter dir paths
     */
    fun getScoreWithDirLength(): Int {
        return score.getTotalScore() + filePath.length * -5
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
        return "FuzzyMatchContainer(basePath='$basePath', filePath='$filePath', score=${getScore()}, dirScore=${getScoreWithDirLength()})"
    }

    /**
     * Serializable version of FuzzyMatchContainer
     *
     * This is required for SerializedMatchContainerConverter to work correctly
     */
    class SerializedMatchContainer : Serializable {
        companion object {
            fun fromFuzzyMatchContainer(container: FuzzyMatchContainer): SerializedMatchContainer {
                val serialized = SerializedMatchContainer()
                serialized.score = container.score
                serialized.filePath = container.filePath
                serialized.filename = container.filename
                serialized.moduleBasePath = container.basePath
                return serialized
            }

            fun fromListModel(listModel: DefaultListModel<FuzzyMatchContainer>): DefaultListModel<SerializedMatchContainer> {
                val serializedList = DefaultListModel<SerializedMatchContainer>()
                for (i in 0 until listModel.size) {
                    serializedList.addElement(fromFuzzyMatchContainer(listModel[i]))
                }
                return serializedList
            }

            fun toListModel(serializedList: DefaultListModel<SerializedMatchContainer>): DefaultListModel<FuzzyMatchContainer> {
                val listModel = DefaultListModel<FuzzyMatchContainer>()
                for (i in 0 until serializedList.size) {
                    listModel.addElement(serializedList[i].toFuzzyMatchContainer())
                }
                return listModel
            }
        }

        fun toFuzzyMatchContainer(): FuzzyMatchContainer {
            return FuzzyMatchContainer(score!!, filePath!!, filename!!, moduleBasePath!!)
        }

        var score: FuzzyScore? = null
        var filePath: String? = null
        var filename: String? = null
        var moduleBasePath: String? = null
    }

    /**
     * This is necessary to persists recently used files between IDE restarts
     *
     * Uses a base 64 encoded string
     *
     * ```
     * @OptionTag(converter = FuzzyMatchContainer.SerializedMatchContainerConverter::class)
     * var recentlySearchedFiles: DefaultListModel<SerializedMatchContainer>? = DefaultListModel()
     * ```
     *
     * @see FuzzierSettingsService
     */
    class SerializedMatchContainerConverter : Converter<DefaultListModel<SerializedMatchContainer>>() {
        override fun fromString(value: String) : DefaultListModel<SerializedMatchContainer> {
            // Fallback to an empty list if deserialization fails
            try {
                val data = Base64.getDecoder().decode(value)
                val byteArrayInputStream = ByteArrayInputStream(data)

                @Suppress("UNCHECKED_CAST")
                return ObjectInputStream(byteArrayInputStream).use { it.readObject() as DefaultListModel<SerializedMatchContainer> }
            } catch (_: Exception) {
                return DefaultListModel<SerializedMatchContainer>()
            }
        }

        override fun toString(value: DefaultListModel<SerializedMatchContainer>) : String {
            val byteArrayOutputStream = ByteArrayOutputStream()
            ObjectOutputStream(byteArrayOutputStream).use { it.writeObject(value) }
            return Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray())
        }
    }
}