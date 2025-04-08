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

import com.intellij.openapi.roots.ContentIterator
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.openapi.vfs.VirtualFile
import com.mituuz.fuzzier.util.FuzzierUtil
import java.util.concurrent.Future
import javax.swing.DefaultListModel

/**
 * Handles creating the content iterators used for string handling and excluding files
 * @param exclusionList exclusion list from settings
 * @param changeListManager handles VCS check if file is being tracked. Null if VCS search should not be used
 */
class StringEvaluator(
    private var exclusionList: Set<String>,
    private var modules: Map<String, String>,
    private var changeListManager: ChangeListManager? = null
) {
    lateinit var scoreCalculator: ScoreCalculator

    fun getContentIterator(
        moduleName: String, searchString: String, listModel: DefaultListModel<FuzzyContainer>,
        task: Future<*>?
    ): ContentIterator {
        scoreCalculator = ScoreCalculator(searchString)
        return ContentIterator { file: VirtualFile ->
            if (task?.isCancelled == true) {
                return@ContentIterator false
            }
            if (!file.isDirectory) {
                val moduleBasePath = modules[moduleName] ?: return@ContentIterator true

                val filePath = file.path.removePrefix(moduleBasePath)
                if (isExcluded(file, filePath)) {
                    return@ContentIterator true
                }
                if (filePath.isNotBlank()) {
                    val fuzzyMatchContainer = createFuzzyContainer(filePath, moduleBasePath, scoreCalculator)
                    if (fuzzyMatchContainer != null) {
                        listModel.addElement(fuzzyMatchContainer)
                    }
                }
            }
            true
        }
    }

    fun getDirIterator(
        moduleName: String, searchString: String, listModel: DefaultListModel<FuzzyContainer>,
        task: Future<*>?
    ): ContentIterator {
        scoreCalculator = ScoreCalculator(searchString)
        return ContentIterator { file: VirtualFile ->
            if (task?.isCancelled == true) {
                return@ContentIterator false
            }
            if (file.isDirectory) {
                val moduleBasePath = modules[moduleName] ?: return@ContentIterator true
                val filePath = getDirPath(file, moduleBasePath, moduleName)
                if (isExcluded(file, filePath)) {
                    return@ContentIterator true
                }
                if (filePath.isNotBlank()) {
                    val fuzzyMatchContainer = createFuzzyContainer(filePath, moduleBasePath, scoreCalculator)
                    if (fuzzyMatchContainer != null) {
                        listModel.addElement(fuzzyMatchContainer)
                    }
                }
            }
            true
        }
    }

    fun evaluateFile(
        iterationFile: FuzzierUtil.IterationFile, listModel: DefaultListModel<FuzzyContainer>,
        searchString: String
    ) {
        val scoreCalculator = ScoreCalculator(searchString)
        val file = iterationFile.file
        val moduleName = iterationFile.module
        if (!file.isDirectory) {
            val moduleBasePath = modules[moduleName] ?: return

            val filePath = file.path.removePrefix(moduleBasePath)
            if (isExcluded(file, filePath)) {
                return
            }
            if (filePath.isNotBlank()) {
                val fuzzyMatchContainer = createFuzzyContainer(filePath, moduleBasePath, scoreCalculator)
                if (fuzzyMatchContainer != null) {
                    listModel.addElement(fuzzyMatchContainer)
                }
            }
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
    private fun isExcluded(file: VirtualFile, filePath: String): Boolean {
        if (changeListManager !== null) {
            return changeListManager!!.isIgnoredFile(file)
        }
        return exclusionList.any { e ->
            when {
                e.startsWith("*") -> filePath.endsWith(e.substring(1))
                e.endsWith("*") -> filePath.startsWith(e.substring(0, e.length - 1))
                else -> filePath.contains(e)
            }
        }
    }

    /**
     * @param filePath to evaluate
     * @return null if no match can be found
     */
    private fun createFuzzyContainer(
        filePath: String, moduleBasePath: String,
        scoreCalculator: ScoreCalculator
    ): FuzzyMatchContainer? {
        val filename = filePath.substring(filePath.lastIndexOf("/") + 1)
        return when (val score = scoreCalculator.calculateScore(filePath)) {
            null -> null
            else -> FuzzyMatchContainer(score, filePath, filename, moduleBasePath)
        }
    }
}