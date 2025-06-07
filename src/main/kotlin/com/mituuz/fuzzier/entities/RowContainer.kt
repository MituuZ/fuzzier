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

import com.google.gson.JsonObject
import com.mituuz.fuzzier.settings.FuzzierGlobalSettingsService
import java.io.File
import kotlin.io.path.Path

class RowContainer(
    filePath: String,
    basePath: String,
    filename: String,
    val rowNumber: Int,
    val columnNumber: Int,
    val trimmedRow: String
) : FuzzyContainer(filePath, basePath, filename) {
    companion object {
        val FILE_SEPARATOR: String = File.separator

        /**
         * Create a row container from a RipGrep JSON object
         */
        fun fromRipGrepJson(json: JsonObject, basePath: String): RowContainer {
            try {
                val data = json.getAsJsonObject("data")
                val path = data.getAsJsonObject("path")
                val filePath = "$FILE_SEPARATOR${path["text"].asString}"
                val filename = filePath.substringAfterLast(FILE_SEPARATOR)

                val lines = data.getAsJsonObject("lines")
                val trimmedRow = lines["text"].asString.trim()
                val rowNumber = data.getAsJsonPrimitive("line_number").asInt - 1
                val columnNumber = if (data.asJsonObject["submatches"].asJsonArray.size() > 0) {
                    data.asJsonObject["submatches"].asJsonArray[0].asJsonObject["start"].asInt - 1
                } else {
                    0
                }

                return RowContainer(filePath, basePath, filename, rowNumber, columnNumber, trimmedRow)
            } catch (e: Exception) {
                throw IllegalArgumentException("Invalid RipGrep JSON: $json", e)
            }
        }

        /**
         * Create a row container from a string
         * <br><br>
         * <h2>grep</h2>
         * ```
         * ./src/main/kotlin/com/mituuz/fuzzier/components/TestBenchComponent.kt:205:            moduleFileIndex.iterateContent(contentIterator)
         * ```
         * <h2>findstr</h2>
         * ```
         * src\main\kotlin\com\mituuz\fuzzier\components\TestBenchComponent.kt:205:            moduleFileIndex.iterateContent(contentIterator)
         * ```
         */
        fun fromString(row: String, basePath: String): RowContainer {
            val parts = row.split(":", limit = 3)

            require(parts.size > 2) {
                throw IllegalArgumentException("Invalid row string: $row")
            }

            var filePath = parts[0].removePrefix(".")
            val filename = filePath.substringAfterLast(FILE_SEPARATOR)
            val rowNumber = parts[1].toInt() - 1
            if (!filePath.startsWith(FILE_SEPARATOR)) {
                filePath = "$FILE_SEPARATOR$filePath"
            }
            val trimmedRow: String = parts[2].trim()
            return RowContainer(filePath, basePath, filename, rowNumber, 0, trimmedRow)
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