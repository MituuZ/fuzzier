package com.mituuz.fuzzier

import com.intellij.openapi.roots.ContentIterator
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.openapi.vfs.VirtualFile
import com.mituuz.fuzzier.entities.FuzzyMatchContainer
import com.mituuz.fuzzier.entities.ScoreCalculator
import javax.swing.DefaultListModel

class StringEvaluator(
    private var multiMatch: Boolean, private var exclusionList: Set<String>, private var matchWeightSingleChar: Int,
    private var matchWeightStreakModifier: Int, private var matchWeightPartialPath: Int, private var changeListManager: ChangeListManager? = null) {
    lateinit var scoreCalculator: ScoreCalculator

    fun getContentIterator(projectBasePath: String, searchString: String, listModel: DefaultListModel<FuzzyMatchContainer>): ContentIterator {
        scoreCalculator = ScoreCalculator(searchString)
        return ContentIterator { file: VirtualFile ->
            if (!file.isDirectory) {
                val filePath = projectBasePath.let { it1 -> file.path.removePrefix(it1) }
                if (isExcluded(file, filePath)) {
                    return@ContentIterator true
                }
                if (filePath.isNotBlank()) {
                    val fuzzyMatchContainer = createFuzzyContainer(filePath)
                    if (fuzzyMatchContainer != null) {
                        listModel.addElement(fuzzyMatchContainer)
                    }
                }
            }
            true
        }
    }

    fun getDirIterator(projectBasePath: String, searchString: String, listModel: DefaultListModel<FuzzyMatchContainer>): ContentIterator {
        scoreCalculator = ScoreCalculator(searchString)
        return ContentIterator { file: VirtualFile ->
            if (file.isDirectory) {
                val filePath = projectBasePath.let { it1 -> file.path.removePrefix(it1) }
                if (isExcluded(file, filePath)) {
                    return@ContentIterator true
                }
                if (filePath.isNotBlank()) {
                    val fuzzyMatchContainer = createFuzzyContainer(filePath)
                    if (fuzzyMatchContainer != null) {
                        listModel.addElement(fuzzyMatchContainer)
                    }
                }
            }
            true
        }
    }

    private fun isExcluded(file: VirtualFile, filePath: String): Boolean {
        if (changeListManager !== null) {
            return changeListManager!!.isIgnoredFile(file)
        }
       for (e in exclusionList) {
            when {
                e.startsWith("*") -> {
                    if (filePath.endsWith(e.substring(1))) {
                        return true
                    }
                }
                e.endsWith("*") -> {
                    if (filePath.startsWith(e.substring(0, e.length - 1))) {
                        return true
                    }
                }
                filePath.contains(e) -> {
                    return true
                }
            }
        }
        return false
    }

    // Returns null if no match can be found
    private fun createFuzzyContainer(filePath: String): FuzzyMatchContainer? {
        val filename = filePath.substring(filePath.lastIndexOf("/") + 1)
        return when (val score = scoreCalculator.calculateScore(filePath)) {
            null -> {
                null
            }

            else -> FuzzyMatchContainer(score, filePath, filename)
        }
    }

    fun setSettings(multiMatch: Boolean, matchWeightSingleChar: Int, matchWeightPartialPath: Int,
                    matchWeightStreakModifier: Int) {
        this.multiMatch = multiMatch
        this.matchWeightPartialPath = matchWeightPartialPath
        this.matchWeightSingleChar = matchWeightSingleChar
        this.matchWeightStreakModifier = matchWeightStreakModifier
    }
}