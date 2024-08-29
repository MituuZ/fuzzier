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
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.FileIndex
import com.intellij.openapi.vfs.VirtualFile
import java.util.concurrent.ConcurrentHashMap

class FuzzierUtil {
    private var settingsState = service<FuzzierSettingsService>().state
    private var listLimit: Int = settingsState.fileListLimit
    private var prioritizeShorterDirPaths = settingsState.prioritizeShorterDirPaths

    data class IterationFile(val file: VirtualFile, val module: String)
    
    companion object {
        fun fileIndexToIterationFiles(iterationFiles: ConcurrentHashMap.KeySetView<IterationFile, Boolean>, 
                                  fileIndex: FileIndex, moduleName: String) {
            fileIndex.iterateContent { file ->
                if (!file.isDirectory) {
                    iterationFiles.add(IterationFile(file, moduleName))
                }
                true
            }
        }
    }

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
        val useShortDirPath = isDirSort && prioritizeShorterDirPaths
        var comparator = getComparator(useShortDirPath, false)
        val priorityQueue = PriorityQueue(listLimit + 1, comparator)

        var minimumScore: Int? = null
        listModel.elements().toList().forEach {
            if (minimumScore == null || it.getScore() > minimumScore!!) {
                priorityQueue.add(it)
                if (priorityQueue.size > listLimit) {
                    priorityQueue.remove()
                    minimumScore = priorityQueue.peek().getScore()
                }
            }
        }

        comparator = getComparator(useShortDirPath, true)
        val result = DefaultListModel<FuzzyMatchContainer>()
        result.addAll(priorityQueue.toList().sortedWith(comparator))

        return result
    }

    private fun getComparator(useShortDirPath: Boolean, isDescending: Boolean): Comparator<FuzzyMatchContainer> {
        return if (isDescending) {
            compareByDescending { if (useShortDirPath) it.getScoreWithDirLength() else it.getScore() }
        } else {
            compareBy { if (useShortDirPath) it.getScoreWithDirLength() else it.getScore() }
        }
    }

    fun setListLimit(listLimit: Int) {
        this.listLimit = listLimit
    }

    fun setPrioritizeShorterDirPaths(prioritizeShortedFilePaths: Boolean) {
        this.prioritizeShorterDirPaths = prioritizeShortedFilePaths;
    }

    fun removeModulePath(filePath: String): Pair<String, String> {
        val modules = settingsState.modules
        for (modulePath in modules.values) {
            if (filePath.contains(modulePath)) {
                val file = filePath.removePrefix(modulePath)
                return Pair(file, modulePath)
            }
        }
        return Pair(filePath, "")
    }

    /**
     * Parse all modules in the project and find the shortest base path for each of them.
     * Combines similar module paths to the shortest possible form.
     *
     * Populates `FuzzierSettings.state.modules`-field
     */
    fun parseModules(project: Project) {
        val moduleManager = ModuleManager.getInstance(project)

        // Gather all modules and paths into a list
        val moduleList: MutableList<ModuleContainer> = ArrayList()
        for (module in moduleManager.modules) {
            val modulePath = getModulePath(module) ?: continue
            moduleList.add(ModuleContainer(module.name, modulePath))
        }

        var prevModule: ModuleContainer? = null
        for (currentModule in moduleList.sortedBy { it.basePath }) {
            if (prevModule != null && (currentModule.basePath.startsWith(prevModule.basePath)
                        || prevModule.basePath.startsWith(currentModule.basePath))) {
                if (currentModule.basePath.length > prevModule.basePath.length) {
                    currentModule.basePath = prevModule.basePath;
                } else {
                    prevModule.basePath = currentModule.basePath;
                }
            }

            prevModule = currentModule
        }

        if (moduleList.map { it.basePath }.distinct().size > 1) {
            shortenModulePaths(moduleList)
        }

        if (moduleList.isEmpty() && project.basePath != null) {
            moduleList.add(ModuleContainer(project.name, project.basePath!!))
            service<FuzzierSettingsService>().state.isProject = true
        }

        val moduleMap = listToMap(moduleList)
        service<FuzzierSettingsService>().state.modules = moduleMap
    }

    private fun shortenModulePaths(modules: List<ModuleContainer>) {
        for (module in modules) {
            module.basePath = module.basePath.substringBeforeLast("/")
        }
    }

    private fun getModulePath(module: Module): String? {
        val contentRoots = module.rootManager.contentRoots
        if (contentRoots.isEmpty()) {
            return null
        }
        return contentRoots.firstOrNull()?.path
    }

    data class ModuleContainer(val name:String, var basePath:String)

    private fun listToMap(modules: List<ModuleContainer>): Map<String, String> {
        return modules.associateBy({ it.name }, { it.basePath })
    }
}