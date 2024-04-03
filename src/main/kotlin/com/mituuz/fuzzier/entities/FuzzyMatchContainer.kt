package com.mituuz.fuzzier.entities

class FuzzyMatchContainer {
    lateinit var score: FuzzyScore
    var filePath: String = ""
    var filename: String = ""
    var intScore: Int = 0

    constructor(score: FuzzyScore, filePath: String, filename: String) {
        this.score = score
        this.filePath = filePath
        this.filename = filename
    }

    // TODO: Remove this once it is no longer necessary
    constructor(score: Int, filePath: String, filename: String) {
        this.intScore = score
        this.filePath = filePath
        this.filename = filename
    }

    fun toString(filenameType: FilenameType): String {
        return when (filenameType) {
            FilenameType.FILENAME_ONLY -> filename
            FilenameType.FILEPATH_ONLY -> filePath
            FilenameType.FILENAME_WITH_PATH -> "$filename   ($filePath)"
        }
    }

    fun getScore(): Int {
        return score.getTotalScore()
    }

    enum class FilenameType(val text: String) {
        FILEPATH_ONLY("Filepath only"),
        FILENAME_ONLY("Filename only"),
        FILENAME_WITH_PATH("Filename with (path)")
    }

    class FuzzyScore {
        var streakScore = 0
        var multiMatchScore = 0
        var partialPathScore = 0
        var filenameScore = 0

        fun getTotalScore(): Int {
            return streakScore + multiMatchScore + partialPathScore + filenameScore
        }
    }
}