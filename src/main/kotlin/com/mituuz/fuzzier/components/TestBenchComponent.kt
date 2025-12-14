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
package com.mituuz.fuzzier.components

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.project.rootManager
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorTextField
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import com.intellij.uiDesigner.core.GridConstraints
import com.intellij.uiDesigner.core.GridLayoutManager
import com.mituuz.fuzzier.entities.*
import com.mituuz.fuzzier.intellij.iteration.IntelliJIterationFileCollector
import com.mituuz.fuzzier.intellij.iteration.IterationFileCollector
import com.mituuz.fuzzier.settings.FuzzierSettingsService
import com.mituuz.fuzzier.util.FuzzierUtil
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.apache.commons.lang3.StringUtils
import java.awt.Dimension
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Future
import javax.swing.DefaultListModel
import javax.swing.JPanel
import javax.swing.table.DefaultTableModel
import kotlin.concurrent.schedule

class TestBenchComponent : JPanel(), Disposable {
    private val columnNames =
        arrayOf("Filename", "Filepath", "Streak", "MultiMatch", "PartialPath", "Filename", "Total")
    private val table = JBTable()
    private var searchField = EditorTextField()
    private var debounceTimer: TimerTask? = null

    @Volatile
    var currentTask: Future<*>? = null
    private lateinit var liveSettingsComponent: FuzzierGlobalSettingsComponent
    private lateinit var projectState: FuzzierSettingsService.State
    private var currentUpdateListContentJob: Job? = null
    private var actionScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var collector: IterationFileCollector = IntelliJIterationFileCollector()

    fun fill(settingsComponent: FuzzierGlobalSettingsComponent) {
        val project = ProjectManager.getInstance().openProjects[0]
        projectState = project.service<FuzzierSettingsService>().state

        val fuzzierUtil = FuzzierUtil()
        fuzzierUtil.parseModules(project)

        liveSettingsComponent = settingsComponent
        layout = GridLayoutManager(2, 1)
        val scrollPane = JBScrollPane()
        scrollPane.setViewportView(table)

        add(
            scrollPane, GridConstraints(
                0,
                0,
                1,
                1,
                GridConstraints.ANCHOR_CENTER,
                GridConstraints.FILL_BOTH,
                GridConstraints.SIZEPOLICY_CAN_SHRINK or GridConstraints.SIZEPOLICY_WANT_GROW,
                GridConstraints.SIZEPOLICY_CAN_SHRINK or GridConstraints.SIZEPOLICY_WANT_GROW,
                Dimension(-1, 300),
                Dimension(-1, -1),
                null,
                0,
                false
            )
        )
        add(
            searchField, GridConstraints(
                1,
                0,
                1,
                1,
                GridConstraints.ANCHOR_WEST,
                GridConstraints.FILL_HORIZONTAL,
                GridConstraints.SIZEPOLICY_WANT_GROW,
                GridConstraints.SIZEPOLICY_FIXED,
                null,
                Dimension(-1, -1),
                null,
                0,
                false
            )
        )

        // Add a listener that updates the search list every time a change is made
        val document = searchField.document
        val listener: DocumentListener = object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                debounceTimer?.cancel()
                val debouncePeriod = liveSettingsComponent.debounceTimerValue.getIntSpinner().value as Int
                debounceTimer = Timer().schedule(debouncePeriod.toLong()) {
                    updateListContents(project, searchField.text)
                }
            }
        }

        document.addDocumentListener(listener, liveSettingsComponent.disposable)
    }

    fun updateListContents(project: Project, searchString: String) {
        if (StringUtils.isBlank(searchString)) {
            ApplicationManager.getApplication().invokeLater {
                table.model = DefaultTableModel()
            }
            return
        }

        // Use live settings from the component (unsaved UI state) so changes are reflected immediately
        val liveGlobalExclusions =
            liveSettingsComponent.globalExclusionTextArea.text.lines().map { it.trim() }.filter { it.isNotEmpty() }
                .toSet()

        val combinedExclusions = buildSet {
            addAll(projectState.exclusionSet)
            addAll(liveGlobalExclusions)
        }

        currentUpdateListContentJob?.cancel()
        currentUpdateListContentJob = actionScope.launch {
            table.setPaintBusy(true)

            try {
                val stringEvaluator = StringEvaluator(
                    combinedExclusions, project.service<FuzzierSettingsService>().state.modules
                )

                val iterationEntries = withContext(Dispatchers.Default) {
                    collectIterationFiles(project)
                }

                val listModel = withContext(Dispatchers.Default) {
                    processIterationEntries(
                        iterationEntries,
                        stringEvaluator,
                        searchString,
                        liveSettingsComponent.fileListLimit.getIntSpinner().value as Int,
                    )
                }

                val sortedList =
                    listModel.elements().toList().sortedByDescending { (it as FuzzyMatchContainer).getScore() }
                val data: Array<Array<Any>> = sortedList.map {
                    arrayOf(
                        (it as FuzzyMatchContainer).filename as Any,
                        it.filePath as Any,
                        it.score.streakScore as Any,
                        it.score.multiMatchScore as Any,
                        it.score.partialPathScore as Any,
                        it.score.filenameScore as Any,
                        it.score.getTotalScore() as Any
                    )
                }.toTypedArray()

                val tableModel = DefaultTableModel(data, columnNames)
                table.model = tableModel
            } finally {
                table.setPaintBusy(false)
            }
        }
    }

    override fun dispose() {
        debounceTimer?.cancel()
        debounceTimer = null

        currentTask?.let { task ->
            if (!task.isDone) task.cancel(true)
        }
        currentTask = null

        ApplicationManager.getApplication().invokeLater {
            try {
                table.setPaintBusy(false)
            } catch (_: Throwable) {
                // Ignore this
            }
        }
    }

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
            targets = indexTargets, shouldContinue = { job.isActive }, fileFilter = buildFileFilter()
        )
    }

    private fun buildFileFilter(): (VirtualFile) -> Boolean = { vf -> !vf.isDirectory }

    suspend fun processIterationEntries(
        fileEntries: List<IterationEntry>,
        stringEvaluator: StringEvaluator,
        searchString: String,
        fileListLimit: Int,
    ): DefaultListModel<FuzzyContainer> {
        val ss = FuzzierUtil.cleanSearchString(searchString, projectState.ignoredCharacters)
        val processedFiles = ConcurrentHashMap.newKeySet<String>()
        val priorityQueue = PriorityQueue(
            fileListLimit + 1, compareBy<FuzzyMatchContainer> { it.getScore() })

        val queueLock = Any()
        var minimumScore: Int? = null

        val cores = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
        val parallelism = (cores - 1).coerceIn(1, 8)

        coroutineScope {
            val ch = Channel<IterationEntry>(capacity = parallelism * 2)

            repeat(parallelism) {
                launch {
                    for (iterationFile in ch) {
                        val matchConfig = MatchConfig(
                            liveSettingsComponent.tolerance.getIntSpinner().value as Int,
                            liveSettingsComponent.multiMatchActive.getCheckBox().isSelected,
                            liveSettingsComponent.matchWeightSingleChar.getIntSpinner().value as Int,
                            liveSettingsComponent.matchWeightStreakModifier.getIntSpinner().value as Int,
                            liveSettingsComponent.matchWeightPartialPath.getIntSpinner().value as Int,
                            liveSettingsComponent.matchWeightFilename.getIntSpinner().value as Int
                        )

                        val container = stringEvaluator.evaluateIteratorEntry(iterationFile, ss, matchConfig)
                        container?.let { fuzzyMatchContainer ->
                            synchronized(queueLock) {
                                minimumScore = priorityQueue.maybeAdd(
                                    minimumScore, fuzzyMatchContainer, fileListLimit
                                )
                            }
                        }
                    }
                }
            }

            fileEntries.filter { processedFiles.add(it.path) }.forEach { ch.send(it) }
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
        minimumScore: Int?, fuzzyMatchContainer: FuzzyMatchContainer, fileListLimit: Int
    ): Int? {
        var ret = minimumScore

        if (minimumScore == null || fuzzyMatchContainer.getScore() > minimumScore) {
            this.add(fuzzyMatchContainer)
            if (this.size > fileListLimit) {
                this.remove()
                ret = this.peek().getScore()
            }
        }

        return ret
    }
}