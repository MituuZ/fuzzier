package com.mituuz.fuzzier.util

import com.intellij.openapi.components.service
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.rootManager
import com.mituuz.fuzzier.entities.FuzzyMatchContainer
import com.mituuz.fuzzier.settings.FuzzierSettingsService
import java.util.*
import javax.swing.DefaultListModel

class FuzzierUtil {
    private var settingsState = service<FuzzierSettingsService>().state
    private var listLimit: Int = settingsState.fileListLimit
    private var prioritizeShorterDirPaths = settingsState.prioritizeShorterDirPaths

    /**
     * Process all the elements in the listModel with a priority queue to limit the size
     * and keeping the data sorted at all times
     *
     * Priority queue's size is limit + 1 to prevent any resizing
     * Only add entries to the queue if they have larger score than the minimum in the queue
     *
     * @param listModel to limit and sort
     * @param isDirSort defaults to false, enables using different sort for directories
     *
     * @return a sorted and sized list model
     */
    fun sortAndLimit(listModel: DefaultListModel<FuzzyMatchContainer>, isDirSort: Boolean = false): DefaultListModel<FuzzyMatchContainer> {
        val priorityQueue = PriorityQueue(listLimit + 1, compareBy(FuzzyMatchContainer::getScore))

        var minimumScore = -1
        listModel.elements().toList().forEach {
            if (it.getScore() > minimumScore) {
                priorityQueue.add(it)
                if (priorityQueue.size > listLimit) {
                    minimumScore = priorityQueue.remove().getScore()
                }
            }
        }

        val result = DefaultListModel<FuzzyMatchContainer>()
        if (isDirSort && prioritizeShorterDirPaths) {
            result.addAll(priorityQueue.toList().sortedByDescending { it.getScoreWithDirLength() })
        } else {
            result.addAll(priorityQueue.toList().sortedByDescending { it.getScore() })
        }

        return result
    }

    fun setListLimit(listLimit: Int) {
        this.listLimit = listLimit
    }

    fun setPrioritizeShorterDirPaths(prioritizeShortedFilePaths: Boolean) {
        this.prioritizeShorterDirPaths = prioritizeShortedFilePaths;
    }

    /**
     * Calculate the number of unique root paths in the project by comparing module paths against each other
     * Handles cases where some modules are nested under a single project root
     *
     * @return true if at least one module has a non-compatible root path, false if all paths can be merged
     */
    fun hasMultipleUniqueRootPaths(moduleManager: ModuleManager): Boolean {
        var currentRoot: String? = null
        for (module in moduleManager.modules) {
            val contentRoots = module.rootManager.contentRoots
            if (contentRoots.isEmpty()) {
                continue
            }
            val moduleBasePath = contentRoots[0]?.path ?: continue

            // Initial case, set the first root
            if (currentRoot == null) {
                currentRoot = moduleBasePath
                continue
            }

            // Check if either root is contained in the other
            if (currentRoot.contains(moduleBasePath) || moduleBasePath.contains(currentRoot)) {
                // Update current root if new root is shorter
                if (moduleBasePath.length < currentRoot.length) {
                    currentRoot = moduleBasePath
                }
            } else {
                // Root wasn't contained, multiple root paths are present
                return true
            }
        }
        return false
    }

}