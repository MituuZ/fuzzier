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

class RowContainer(
    filePath: String,
    basePath: String,
    filename: String,
    val rowNumber: Int,
    val columnNumber: Int,
    val trimmedRow: String
) : FuzzyContainer(filePath, basePath, filename) {
    companion object {
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
        fun rowContainerFromString(row: String, basePath: String, isRg: Boolean, isWindows: Boolean): RowContainer {
            val parts = row.split(":")
            var filePath = parts[0].removePrefix(".")
            val filename = filePath.substringAfterLast(if (isWindows) "\\" else "/")
            val rowNumber = parts[1].toInt()
            val columnNumber: Int
            val trimmedRow: String
            if (isRg) {
                columnNumber = parts[2].toInt()
                trimmedRow = parts[3]
            } else {
                if (isWindows) {
                    filePath = "/$filePath"
                }
                columnNumber = 0
                trimmedRow = parts[2]
            }
            return RowContainer(filePath, basePath, filename, rowNumber, columnNumber, trimmedRow)
        }
    }

    override fun getDisplayString(state: FuzzierGlobalSettingsService.State): String {
        if (state.filenameType == FilenameType.DEBUG) {
            return toString()
        }
        return "$filename $rowNumber:$columnNumber:$trimmedRow"
    }

    override fun toString(): String {
        return "RowContainer(basePath='$basePath', filePath='$filePath', filename='$filename', rowNumber=$rowNumber)"
    }
}