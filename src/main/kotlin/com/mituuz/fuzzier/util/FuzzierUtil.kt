/*
MIT License

Copyright (c) 2024 Mitja Leino

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
package com.mituuz.fuzzier.util

import com.intellij.openapi.components.service
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.rootManager
import com.mituuz.fuzzier.entities.FuzzyMatchContainer
import com.mituuz.fuzzier.settings.FuzzierSettingsService
import java.util.*
import javax.swing.DefaultListModel
import kotlin.collections.ArrayList

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

    fun getUniqueModulePaths(project: Project): List<String> {
        val moduleManager = ModuleManager.getInstance(project)
        val uniqueModuleRoots = ArrayList<String>()
        for (module in moduleManager.modules) {
            val contentRoots = module.rootManager.contentRoots
            if (contentRoots.isEmpty()) {
                continue
            }
            val moduleBasePath = contentRoots[0]?.path ?: continue

            if (uniqueModuleRoots.isEmpty()) {
                uniqueModuleRoots.add(moduleBasePath)
                continue
            }

            for (root in uniqueModuleRoots) {
                if (moduleBasePath.contains(root) || root.contains(moduleBasePath)) {
                    if (moduleBasePath.length < root.length) {
                        uniqueModuleRoots.remove(root)
                        uniqueModuleRoots.add(moduleBasePath)
                    }
                    continue
                }
            }

            uniqueModuleRoots.add(moduleBasePath)
        }

        return uniqueModuleRoots
    }

    fun removeModulePath(filePath: String, modulePaths: List<String>): Pair<String, String> {
        for (modulePath in modulePaths) {
            if (filePath.contains(modulePath)) {
                return Pair(filePath.removePrefix(modulePath), modulePath)
            }
        }
        return Pair(filePath, "")
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