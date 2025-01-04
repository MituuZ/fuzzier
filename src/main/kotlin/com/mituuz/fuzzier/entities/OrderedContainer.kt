package com.mituuz.fuzzier.entities

class OrderedContainer(filePath: String, basePath: String, filename: String) :
    FuzzyContainer(filePath, basePath, filename) {

    override fun getDisplayString(filenameType: FilenameType, highlight: Boolean): String {
        return when (filenameType) {
            FilenameType.FILENAME_ONLY -> filename
            FilenameType.FILE_PATH_ONLY -> filePath
            FilenameType.FILENAME_WITH_PATH -> "$filename   ($filePath)"
            FilenameType.FILENAME_WITH_PATH_STYLED -> getFilenameWithPathStyled()
        }
    }

    override fun toString(): String {
        return "OrderedContainer(filePath='$filePath', basePath='$basePath', filename='$filename')"
    }
}