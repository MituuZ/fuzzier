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

import com.mituuz.fuzzier.settings.FuzzierGlobalSettingsService
import java.io.File

class RowContainer(
    filePath: String,
    basePath: String,
    filename: String,
    val rowNumber: Int,
    val columnNumber: Int,
    val trimmedRow: String
) : FuzzyContainer(filePath, basePath, filename) {
    companion object {
        private val FILE_SEPARATOR: String = File.separator
        private val RG_PATTERN: Regex = Regex("""^.+:\d+:\d+:\s*.+$""")
        private val COMMON_PATTERN: Regex = Regex("""^.+:\d+:\s*.+$""")

        /**
         * Create a row container from a string
         * <br><br>
         * <h2>ripgrep</h2>
         * ```
         * ./src/main/kotlin/com/mituuz/fuzzier/components/TestBenchComponent.kt:205:33:            moduleFileIndex.iterateContent(contentIterator)
         * ```
         * <h2>grep</h2>
         * ```
         * ./src/main/kotlin/com/mituuz/fuzzier/components/TestBenchComponent.kt:205:            moduleFileIndex.iterateContent(contentIterator)
         * ```
         * <h2>findstr</h2>
         * ```
         * src\main\kotlin\com\mituuz\fuzzier\components\TestBenchComponent.kt:205:            moduleFileIndex.iterateContent(contentIterator)
         * ```
         */
        fun rowContainerFromString(row: String, basePath: String, isRg: Boolean): RowContainer? {
            if (!row.matches(if (isRg) RG_PATTERN else COMMON_PATTERN)) {
                return null
            }

            val parts = getParts(row, isRg)
            if (parts.size != if (isRg) 4 else 3) {
                return null
            }

            var filePath = parts[0].removePrefix(".")
            val filename = filePath.substringAfterLast(FILE_SEPARATOR)
            val rowNumber = parts[1].toInt() - 1
            val columnNumber: Int
            val trimmedRow: String
            if (isRg) {
                columnNumber = parts[2].toInt() - 1
                trimmedRow = parts[3].trim()
            } else {
                if (!filePath.startsWith(FILE_SEPARATOR)) {
                    filePath = "$FILE_SEPARATOR$filePath"
                }
                columnNumber = 0
                trimmedRow = parts[2].trim()
            }
            return RowContainer(filePath, basePath, filename, rowNumber, columnNumber, trimmedRow)
        }

        private fun getParts(row: String, isRg: Boolean): List<String> {
            return if (isRg) {
                row.split(":", limit = 4)
            } else {
                row.split(":", limit = 3)
            }
        }
    }

    override fun getDisplayString(state: FuzzierGlobalSettingsService.State): String {
        if (state.filenameType == FilenameType.DEBUG) {
            return toString()
        }
        return "$filename:$rowNumber:$columnNumber: $trimmedRow"
    }

    override fun toString(): String {
        return "RowContainer(basePath='$basePath', filePath='$filePath', filename='$filename', rowNumber=$rowNumber)"
    }
}