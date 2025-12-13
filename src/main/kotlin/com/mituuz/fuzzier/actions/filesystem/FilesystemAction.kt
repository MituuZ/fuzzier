/*
 *  MIT License
 *
 *  Copyright (c) 2025 Mitja Leino
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package com.mituuz.fuzzier.actions.filesystem

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.EDT
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.rootManager
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.runtime.loader.IntellijLoader.launch
import com.mituuz.fuzzier.actions.FuzzyAction
import com.mituuz.fuzzier.entities.FuzzyContainer
import com.mituuz.fuzzier.entities.FuzzyMatchContainer
import com.mituuz.fuzzier.entities.IterationEntry
import com.mituuz.fuzzier.entities.StringEvaluator
import com.mituuz.fuzzier.intellij.iteration.IntelliJIterationFileCollector
import com.mituuz.fuzzier.intellij.iteration.IterationFileCollector
import com.mituuz.fuzzier.util.FuzzierUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.PriorityQueue
import java.util.concurrent.ConcurrentHashMap
import javax.swing.DefaultListModel

abstract class FilesystemAction : FuzzyAction() {
    private var collector: IterationFileCollector = IntelliJIterationFileCollector()

    abstract override fun runAction(
        project: Project,
        actionEvent: AnActionEvent
    )

    abstract override fun createPopup(screenDimensionKey: String): JBPopup

    abstract fun buildFileFilter(project: Project): (VirtualFile) -> Boolean

    abstract fun handleEmptySearchString(project: Project)

    suspend fun collectIterationFiles(project: Project): List<IterationEntry> {
        val ctx = currentCoroutineContext()
        val job = ctx.job

        val indexTargets = if (projectState.isProject) {
            listOf(ProjectFileIndex.getInstance(project) to project.name)
        } else {
            val moduleManager = ModuleManager.getInstance(project)
            moduleManager.modules.map { it.rootManager.fileIndex to it.name }
        }

        return collector.collectFiles(
            targets = indexTargets,
            shouldContinue = { job.isActive },
            fileFilter = buildFileFilter(project)
        )
    }

    fun getStringEvaluator(): StringEvaluator {
        val combinedExclusions = buildSet {
            addAll(projectState.exclusionSet)
            addAll(globalState.globalExclusionSet)
        }
        return StringEvaluator(
            combinedExclusions,
            projectState.modules,
        )
    }

    /**
     * Processes a set of IterationFiles concurrently
     * @return a priority list which has been size limited and sorted
     */
    suspend fun processIterationEntries(
        fileEntries: List<IterationEntry>,
        stringEvaluator: StringEvaluator,
        searchString: String
    ): DefaultListModel<FuzzyContainer> {
        val ss = FuzzierUtil.Companion.cleanSearchString(searchString, projectState.ignoredCharacters)
        val processedFiles = ConcurrentHashMap.newKeySet<String>()
        val listLimit = globalState.fileListLimit
        val priorityQueue = PriorityQueue(
            listLimit + 1,
            compareBy<FuzzyMatchContainer> { it.getScore() }
        )

        val queueLock = Any()
        var minimumScore: Int? = null

        val cores = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
        val parallelism = (cores - 1).coerceIn(1, 8)

        coroutineScope {
            val ch = Channel<IterationEntry>(capacity = parallelism * 2)

            repeat(parallelism) {
                launch {
                    for (iterationFile in ch) {
                        val container = stringEvaluator.evaluateIteratorEntry(iterationFile, ss)
                        container?.let { fuzzyMatchContainer ->
                            synchronized(queueLock) {
                                minimumScore = priorityQueue.maybeAdd(minimumScore, fuzzyMatchContainer)
                            }
                        }
                    }
                }
            }

            fileEntries
                .filter { processedFiles.add(it.path) }
                .forEach { ch.send(it) }
            ch.close()
        }


        val result = DefaultListModel<FuzzyContainer>()
        result.addAll(
            priorityQueue.sortedWith(
                compareByDescending<FuzzyMatchContainer> { it.getScore() })
        )
        return result
    }

    private fun PriorityQueue<FuzzyMatchContainer>.maybeAdd(
        minimumScore: Int?,
        fuzzyMatchContainer: FuzzyMatchContainer
    ): Int? {
        var ret = minimumScore

        if (minimumScore == null || fuzzyMatchContainer.getScore() > minimumScore) {
            this.add(fuzzyMatchContainer)
            if (this.size > globalState.fileListLimit) {
                this.remove()
                ret = this.peek().getScore()
            }
        }

        return ret
    }

    override fun updateListContents(project: Project, searchString: String) {
        if (searchString.isEmpty()) {
            handleEmptySearchString(project)
            return
        }

        currentUpdateListContentJob?.cancel()
        currentUpdateListContentJob = actionScope?.launch(Dispatchers.EDT) {
            component.fileList.setPaintBusy(true)

            try {
                val stringEvaluator = getStringEvaluator()
                coroutineContext.ensureActive()

                val iterationEntries = withContext(Dispatchers.Default) {
                    collectIterationFiles(project)
                }
                coroutineContext.ensureActive()

                val listModel = withContext(Dispatchers.Default) {
                    processIterationEntries(iterationEntries, stringEvaluator, searchString)
                }
                coroutineContext.ensureActive()

                component.refreshModel(listModel, getCellRenderer())
            } finally {
                component.fileList.setPaintBusy(false)
            }
        }
    }

}