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

abstract class FuzzyContainer(val filePath: String, val basePath: String, val filename: String) {
    /**
     * Get display string for the popup
     */
    abstract fun getDisplayString(state: FuzzierGlobalSettingsService.State): String

    /**
     * Get the complete URI for the file
     */
    fun getFileUri() : String {
        return "$basePath$filePath"
    }

    /**
     * Directories always return full path
     */
    fun getDirDisplayString(): String {
        return filePath
    }

    /**
     * Display string options
     */
    enum class FilenameType(val text: String) {
        FILE_PATH_ONLY("File path only"),
        FILENAME_ONLY("Filename only"),
        FILENAME_WITH_PATH("Filename with (path)"),
        FILENAME_WITH_PATH_STYLED("Filename with (path) styled"),
        DEBUG("Debug information")
    }

    override fun toString(): String {
        return "FuzzyContainer(filePath='$filePath', basePath='$basePath', filename='$filename')"
    }
}