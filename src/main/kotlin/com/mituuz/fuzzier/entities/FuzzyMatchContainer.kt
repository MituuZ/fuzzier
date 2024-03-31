package com.mituuz.fuzzier.entities

class FuzzyMatchContainer {
    var score: Int = 0
    var filePath: String = ""
    var filename: String = ""
    var fuzzyScore: FuzzyScore = FuzzyScore()

    constructor(score: Int, filePath: String, filename: String) {
        this.score = score
        this.filePath = filePath
        this.filename = filename
    }

    constructor(filePath: String, searchString: String) {

    }

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
    }
}