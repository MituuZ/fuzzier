package com.mituuz.fuzzier

import com.intellij.openapi.roots.ContentIterator
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.openapi.vfs.VirtualFile
import com.mituuz.fuzzier.entities.FuzzyMatchContainer
import com.mituuz.fuzzier.entities.ScoreCalculator
import javax.swing.DefaultListModel

/**
 * Handles creating the content iterators used for string handling and excluding files
 * @param exclusionList exclusion list from settings
 * @param changeListManager handles VCS check if file is being tracked. Null if VCS search should not be used
 */
class StringEvaluator(
    private var exclusionList: Set<String>,
    private var changeListManager: ChangeListManager? = null
) {
    lateinit var scoreCalculator: ScoreCalculator

    fun getContentIterator(basePath: String, moduleName: String, isMultiModal: Boolean, searchString: String, listModel: DefaultListModel<FuzzyMatchContainer>): ContentIterator {
        scoreCalculator = ScoreCalculator(searchString)
        return ContentIterator { file: VirtualFile ->
            if (!file.isDirectory) {
                val filePath = basePath.let { it1 -> file.path.removePrefix(it1) }
                if (isExcluded(file, filePath, isMultiModal)) {
                    return@ContentIterator true
                }
                if (filePath.isNotBlank()) {
                    val fuzzyMatchContainer = createFuzzyContainer(filePath, moduleName)
                    if (fuzzyMatchContainer != null) {
                        listModel.addElement(fuzzyMatchContainer)
                    }
                }
            }
            true
        }
    }

    fun getDirIterator(basePath: String, moduleName: String, isMultiModal: Boolean, searchString: String, listModel: DefaultListModel<FuzzyMatchContainer>): ContentIterator {
        scoreCalculator = ScoreCalculator(searchString)
        return ContentIterator { file: VirtualFile ->
            if (file.isDirectory) {
                val filePath = getDirPath(file, basePath, moduleName)
                if (isExcluded(file, filePath, isMultiModal)) {
                    return@ContentIterator true
                }
                if (filePath.isNotBlank()) {
                    val fuzzyMatchContainer = createFuzzyContainer(filePath, moduleName)
                    if (fuzzyMatchContainer != null) {
                        listModel.addElement(fuzzyMatchContainer)
                    }
                }
            }
            true
        }
    }

    private fun getDirPath(virtualFile: VirtualFile, basePath: String, module: String): String {
        var res = virtualFile.path.removePrefix(basePath)
        // Handle project root as a special case
        if (res == "") {
            res = "/"
        }
        if (res == "/$module") {
            res = "/$module/"
        }
        return res
    }

    /**
     * Checks if file should be excluded from the results.
     *
     * If change list manager is set, only use it to decide if file should be ignored.
     *
     * Otherwise, use exclusion list from settings.
     *
     * @param file virtual file to check with change list manager
     * @param filePath to check against the exclusion list
     *
     * @return true if file should be excluded
     */
    private fun isExcluded(file: VirtualFile, filePath: String, isMultiModal: Boolean): Boolean {
        var evPath = filePath
        if (isMultiModal) {
            evPath = "/" + evPath.split("/").drop(2).joinToString("/")
        }
        if (changeListManager !== null) {
            return changeListManager!!.isIgnoredFile(file)
        }
        return exclusionList.any { e ->
            when {
                e.startsWith("*") -> evPath.endsWith(e.substring(1))
                e.endsWith("*") -> evPath.startsWith(e.substring(0, e.length - 1))
                else -> evPath.contains(e)
            }
        }
    }

    /**
     * @param filePath to evaluate
     * @return null if no match can be found
     */
    private fun createFuzzyContainer(filePath: String, module: String): FuzzyMatchContainer? {
        val filename = filePath.substring(filePath.lastIndexOf("/") + 1)
        return when (val score = scoreCalculator.calculateScore(filePath)) {
            null -> {
                null
            }

            else -> FuzzyMatchContainer(score, filePath, filename, module)
        }
    }
}