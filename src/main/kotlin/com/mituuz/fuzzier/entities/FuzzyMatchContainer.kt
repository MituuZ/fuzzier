package com.mituuz.fuzzier.entities

class FuzzyMatchContainer (val score: Int, val filePath: String, val filename: String) {
    fun toString(filenameType: FilenameType): String {
        return when (filenameType) {
            FilenameType.FILENAME_ONLY -> filename
            FilenameType.FILEPATH_ONLY -> filePath
            FilenameType.FILENAME_WITH_PATH -> "$filename   ($filePath)"
        }
    }

    enum class FilenameType(val text: String) {
        FILEPATH_ONLY("Filepath only"),
        FILENAME_ONLY("Filename only"),
        FILENAME_WITH_PATH("Filename with (path)")
    }

    class FuzzyScore {
        fun getScore(): Int {
            return 0
        }
    }
}