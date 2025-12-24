/*
 *  MIT License
 *
 *  Copyright (c) 2025 Mitja Leino
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */
package com.mituuz.fuzzier.entities

import com.intellij.openapi.vfs.VirtualFile
import com.mituuz.fuzzier.settings.FuzzierGlobalSettingsService
import java.io.File

class RowContainer(
    filePath: String,
    basePath: String,
    filename: String,
    val rowNumber: Int,
    val trimmedRow: String,
    val columnNumber: Int = 0,
    virtualFile: VirtualFile? = null
) : FuzzyContainer(filePath, basePath, filename, virtualFile) {
    companion object {
        private val FILE_SEPARATOR: String = File.separator
        private val RG_PATTERN: Regex = Regex("""^.+:\d+:\d+:\s*.+$""")
        private val COMMON_PATTERN: Regex = Regex("""^.+:\d+:\s*.+$""")

        fun rowContainerFromString(row: String, basePath: String): RowContainer? {
            if (!row.matches(COMMON_PATTERN)) {
                return null
            }

            val (driveLetter, adjustedRow) = extractWindowsDriveLetter(row)
            val parts = adjustedRow.split(":", limit = 3)
            val filePath = getFilePath(driveLetter + parts[0])
            val filename = filePath.replace('\\', '/').substringAfterLast('/')
            val rowNumber = parts[1].toInt() - 1
            val trimmedRow: String = parts[2].trim()
            return RowContainer(filePath, basePath, filename, rowNumber, trimmedRow)
        }

        fun rgRowContainerFromString(row: String, basePath: String): RowContainer? {
            if (!row.matches(RG_PATTERN)) {
                return null
            }

            val (driveLetter, adjustedRow) = extractWindowsDriveLetter(row)
            val parts = adjustedRow.split(":", limit = 4)
            val filePath = getFilePath(driveLetter + parts[0])
            val filename = filePath.replace('\\', '/').substringAfterLast('/')
            val rowNumber = parts[1].toInt() - 1
            val columnNumber: Int = parts[2].toInt() - 1
            val trimmedRow: String = parts[3].trim()

            return RowContainer(filePath, basePath, filename, rowNumber, trimmedRow, columnNumber)
        }

        private fun extractWindowsDriveLetter(row: String): Pair<String, String> {
            if (row.length >= 2 && row[0].isLetter() && row[1] == ':') {
                return Pair(row.substring(0, 2), row.substring(2))
            }
            return Pair("", row)
        }

        private fun getFilePath(filePath: String): String {
            val filePath = filePath.removePrefix(".")
            if (!filePath.startsWith(FILE_SEPARATOR) && !filePath.matches(Regex("^[A-Za-z]:.*"))) {
                return "$FILE_SEPARATOR$filePath"
            }

            return filePath
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