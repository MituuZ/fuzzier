package com.mituuz.fuzzier.entities

abstract class FuzzyContainer(val filePath: String, val basePath: String, val filename: String) {
    /**
     * Get display string for the popup
     */
    abstract fun getDisplayString(filenameType: FilenameType, highlight: Boolean): String

    /**
     * Get the complete URI for the file
     */
    open fun getFileUri() : String {
        return "$basePath$filePath"
    }

    /**
     * Display string options
     */
    enum class FilenameType(val text: String) {
        FILE_PATH_ONLY("File path only"),
        FILENAME_ONLY("Filename only"),
        FILENAME_WITH_PATH("Filename with (path)"),
        FILENAME_WITH_PATH_STYLED("Filename with (path) styled")
    }

    fun getFilenameWithPathStyled(): String {
        return "<html><strong>$filename</strong>  <i>($filePath)</i></html>"
    }

    override fun toString(): String {
        return "FuzzyContainer(filePath='$filePath', basePath='$basePath', filename='$filename')"
    }
}