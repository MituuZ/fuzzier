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
package com.mituuz.fuzzier

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.impl.EditorHistoryManager
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.rootManager
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupListener
import com.intellij.openapi.ui.popup.LightweightWindowEvent
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.util.SingleAlarm
import com.mituuz.fuzzier.components.FuzzyFinderComponent
import com.mituuz.fuzzier.entities.FuzzyContainer
import com.mituuz.fuzzier.entities.FuzzyMatchContainer
import com.mituuz.fuzzier.entities.IterationFile
import com.mituuz.fuzzier.entities.StringEvaluator
import com.mituuz.fuzzier.intellij.iteration.IntelliJIterationFileCollector
import com.mituuz.fuzzier.intellij.iteration.IterationFileCollector
import com.mituuz.fuzzier.settings.FuzzierGlobalSettingsService.RecentFilesMode.*
import com.mituuz.fuzzier.util.FuzzierUtil
import com.mituuz.fuzzier.util.InitialViewHandler
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.apache.commons.lang3.StringUtils
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import javax.swing.AbstractAction
import javax.swing.DefaultListModel
import javax.swing.JComponent
import javax.swing.KeyStroke

open class Fuzzier : FuzzyAction() {
    override var popupTitle = "Fuzzy Search"
    override var dimensionKey = "FuzzySearchPopup"
    private var currentUpdateListContentJob: Job? = null
    private var actionScope: CoroutineScope? = null
    private var previewAlarm: SingleAlarm? = null
    private var lastPreviewKey: String? = null
    private var collector: IterationFileCollector = IntelliJIterationFileCollector()

    override fun runAction(project: Project, actionEvent: AnActionEvent) {
        setCustomHandlers()

        actionScope?.cancel()
        actionScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        ApplicationManager.getApplication().invokeLater {
            defaultDoc = EditorFactory.getInstance().createDocument("")
            component = FuzzyFinderComponent(project)
            previewAlarm = getPreviewAlarm()
            createListeners(project)
            showPopup(project)
            createSharedListeners(project)

            (component as FuzzyFinderComponent).splitPane.dividerLocation =
                globalState.splitPosition

            if (globalState.recentFilesMode != NONE) {
                createInitialView(project)
            }
        }
    }

    override fun createPopup(screenDimensionKey: String): JBPopup {
        val popup = getInitialPopup(screenDimensionKey)

        popup.addListener(object : JBPopupListener {
            override fun onClosed(event: LightweightWindowEvent) {
                globalState.splitPosition =
                    (component as FuzzyFinderComponent).splitPane.dividerLocation

                resetOriginalHandlers()

                currentUpdateListContentJob?.cancel()
                currentUpdateListContentJob = null

                actionScope?.cancel()

                previewAlarm?.dispose()
                lastPreviewKey = null
            }
        })

        return popup
    }

    private fun createInitialView(project: Project) {
        component.fileList.setPaintBusy(true)
        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                val editorHistoryManager = EditorHistoryManager.getInstance(project)

                val listModel = when (globalState.recentFilesMode) {
                    RECENT_PROJECT_FILES -> InitialViewHandler.getRecentProjectFiles(
                        globalState,
                        fuzzierUtil,
                        editorHistoryManager,
                        project
                    )

                    RECENTLY_SEARCHED_FILES -> InitialViewHandler.getRecentlySearchedFiles(projectState)
                    else -> {
                        DefaultListModel<FuzzyContainer>()
                    }
                }

                ApplicationManager.getApplication().invokeLater {
                    component.refreshModel(listModel, getCellRenderer())
                }
            } finally {
                component.fileList.setPaintBusy(false)
            }
        }
    }

    override fun updateListContents(project: Project, searchString: String) {
        if (StringUtils.isBlank(searchString)) {
            handleEmptySearchString(project)
            return
        }

        currentUpdateListContentJob?.cancel()
        currentUpdateListContentJob = actionScope?.launch(Dispatchers.EDT) {
            // Create a reference to the current task to check if it has been cancelled
            component.fileList.setPaintBusy(true)

            try {
                val stringEvaluator = getStringEvaluator()
                coroutineContext.ensureActive()

                val iterationFiles = withContext(Dispatchers.Default) {
                    collectIterationFiles(project)
                }
                coroutineContext.ensureActive()

                val listModel = withContext(Dispatchers.Default) {
                    processFiles(iterationFiles, stringEvaluator, searchString)
                }
                coroutineContext.ensureActive()

                component.refreshModel(listModel, getCellRenderer())
            } finally {
                component.fileList.setPaintBusy(false)
            }
        }
    }

    private fun handleEmptySearchString(project: Project) {
        if (globalState.recentFilesMode != NONE) {
            createInitialView(project)
        } else {
            ApplicationManager.getApplication().invokeLater {
                component.fileList.model = DefaultListModel()
                defaultDoc?.let { (component as FuzzyFinderComponent).previewPane.updateFile(it) }
            }
        }
    }

    private fun getStringEvaluator(): StringEvaluator {
        val combinedExclusions = buildSet {
            addAll(projectState.exclusionSet)
            addAll(globalState.globalExclusionSet)
        }
        return StringEvaluator(
            combinedExclusions,
            projectState.modules,
        )
    }

    private suspend fun collectIterationFiles(project: Project): List<IterationFile> {
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

    protected open fun buildFileFilter(project: Project): (VirtualFile) -> Boolean =
        { vf -> !vf.isDirectory }

    /**
     * Processes a set of IterationFiles concurrently
     * @return a priority list which has been size limited and sorted
     */
    private suspend fun processFiles(
        iterationFiles: List<IterationFile>,
        stringEvaluator: StringEvaluator,
        searchString: String
    ): DefaultListModel<FuzzyContainer> {
        val ss = FuzzierUtil.cleanSearchString(searchString, projectState.ignoredCharacters)
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
            val ch = Channel<IterationFile>(capacity = parallelism * 2)

            repeat(parallelism) {
                launch {
                    for (iterationFile in ch) {
                        val container = stringEvaluator.evaluateFile(iterationFile, ss)
                        container?.let { fuzzyMatchContainer ->
                            synchronized(queueLock) {
                                minimumScore = priorityQueue.maybeAdd(minimumScore, fuzzyMatchContainer)
                            }
                        }
                    }
                }
            }

            for (iterationFile in iterationFiles) {
                if (!processedFiles.add(iterationFile.path)) continue
                ch.send(iterationFile)
            }
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

    private fun openFile(project: Project, fuzzyContainer: FuzzyContainer?, virtualFile: VirtualFile) {
        val fileEditorManager = FileEditorManager.getInstance(project)
        val currentEditor = fileEditorManager.selectedTextEditor
        val previousFile = currentEditor?.virtualFile

        if (fileEditorManager.isFileOpen(virtualFile)) {
            fileEditorManager.openFile(virtualFile, true)
        } else {
            fileEditorManager.openFile(virtualFile, true)
            if (currentEditor != null && !globalState.newTab) {
                fileEditorManager.selectedEditor?.let {
                    if (previousFile != null) {
                        fileEditorManager.closeFile(previousFile)
                    }
                }
            }
        }
        if (fuzzyContainer != null) {
            InitialViewHandler.addFileToRecentlySearchedFiles(fuzzyContainer, projectState, globalState)
        }
        popup.cancel()
    }

    private fun createListeners(project: Project) {
        // Add a listener that updates the contents of the preview pane
        component.fileList.addListSelectionListener { event ->
            if (event.valueIsAdjusting) {
                return@addListSelectionListener
            } else {
                previewAlarm?.cancelAndRequest()
            }
        }

        // Add a mouse listener for double-click
        component.fileList.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 2) {
                    val selectedValue = component.fileList.selectedValue
                    val virtualFile =
                        VirtualFileManager.getInstance().findFileByUrl("file://${selectedValue?.getFileUri()}")
                    // Open the file in the editor
                    virtualFile?.let {
                        openFile(project, selectedValue, it)
                    }
                }
            }
        })

        // Add a listener that opens the currently selected file when pressing enter (focus on the text box)
        val enterKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0)
        val enterActionKey = "openFile"
        val inputMap = component.searchField.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
        inputMap.put(enterKeyStroke, enterActionKey)
        component.searchField.actionMap.put(enterActionKey, object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent?) {
                val selectedValue = component.fileList.selectedValue
                val virtualFile =
                    VirtualFileManager.getInstance().findFileByUrl("file://${selectedValue?.getFileUri()}")
                virtualFile?.let {
                    openFile(project, selectedValue, it)
                }
            }
        })
    }

    private fun getPreviewAlarm(): SingleAlarm {
        return SingleAlarm(
            {
                val fuzzyFinderComponent = (component as FuzzyFinderComponent)
                val selected = component.fileList.selectedValue

                if (selected == null || component.fileList.isEmpty) {
                    defaultDoc?.let { fuzzyFinderComponent.previewPane.updateFile(it) }
                    lastPreviewKey = null
                    return@SingleAlarm
                }

                val fileUrl = "file://${selected.getFileUri()}"
                if (fileUrl == lastPreviewKey) return@SingleAlarm
                lastPreviewKey = fileUrl

                val vf = VirtualFileManager.getInstance().findFileByUrl(fileUrl)
                if (vf != null) fuzzyFinderComponent.previewPane.updateFile(vf)
            },
            75,
        )
    }
}