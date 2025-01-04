package com.mituuz.fuzzier.entities

class RowContainer(
    filePath: String,
    basePath: String,
    filename: String,
    val rowNumber: Integer
) : FuzzyContainer(filePath, basePath, filename) {

    override fun getDisplayString(
        filenameType: FilenameType,
        highlight: Boolean
    ): String {
        return when (filenameType) {
            FilenameType.FILENAME_ONLY -> filename
            FilenameType.FILE_PATH_ONLY -> filePath
            FilenameType.FILENAME_WITH_PATH -> "$filename   ($filePath)"
            FilenameType.FILENAME_WITH_PATH_STYLED -> getFilenameWithPathStyled()
        }
    }

    override fun toString(): String {
        return "RowContainer(filePath='$filePath', basePath='$basePath', filename='$filename', rowNumber=$rowNumber)"
    }
}