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
package com.mituuz.fuzzier

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.impl.EditorHistoryManager
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.rootManager
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupListener
import com.intellij.openapi.ui.popup.LightweightWindowEvent
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.mituuz.fuzzier.components.FuzzyFinderComponent
import com.mituuz.fuzzier.entities.FuzzyContainer
import com.mituuz.fuzzier.entities.StringEvaluator
import com.mituuz.fuzzier.settings.FuzzierGlobalSettingsService.RecentFilesMode.NONE
import com.mituuz.fuzzier.settings.FuzzierGlobalSettingsService.RecentFilesMode.RECENTLY_SEARCHED_FILES
import com.mituuz.fuzzier.settings.FuzzierGlobalSettingsService.RecentFilesMode.RECENT_PROJECT_FILES
import com.mituuz.fuzzier.util.FuzzierUtil
import com.mituuz.fuzzier.util.InitialViewHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.apache.commons.lang3.StringUtils
import java.awt.event.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Future
import javax.swing.*
import kotlin.coroutines.cancellation.CancellationException

open class Fuzzier : FuzzyAction() {
    override var popupTitle = "Fuzzy Search"
    override var dimensionKey = "FuzzySearchPopup"

    // Used by FuzzierVCS to check if files are tracked by the VCS
    protected var changeListManager: ChangeListManager? = null

    override fun runAction(project: Project, actionEvent: AnActionEvent) {
        setCustomHandlers()

        ApplicationManager.getApplication().invokeLater {
            defaultDoc = EditorFactory.getInstance().createDocument("")
            component = FuzzyFinderComponent(project)
            createListeners(project)
            createSharedListeners(project)

            showPopup(project)

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
                super.onClosed(event)
            }
        })

        return popup
    }

    /**
     * Populates the file list with recently opened files
     */
    private fun createInitialView(project: Project) {
        ApplicationManager.getApplication().executeOnPooledThread {
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
                component.fileList.model = listModel
                component.fileList.cellRenderer = getCellRenderer()
                component.fileList.setPaintBusy(false)
                if (!component.fileList.isEmpty) {
                    component.fileList.setSelectedValue(listModel[0], true)
                }
            }
        }
    }

    override fun updateListContents(project: Project, searchString: String) {
        if (StringUtils.isBlank(searchString)) {
            handleEmptySearchString(project)
            return
        }

        currentTask?.takeIf { !it.isDone }?.cancel(true)
        currentTask = ApplicationManager.getApplication().executeOnPooledThread {
            try {
                // Create a reference to the current task to check if it has been cancelled
                val task = currentTask
                component.fileList.setPaintBusy(true)
                var listModel = DefaultListModel<FuzzyContainer>()

                val stringEvaluator = getStringEvaluator()

                if (task?.isCancelled == true) return@executeOnPooledThread

                process(project, stringEvaluator, searchString, listModel, task)

                if (task?.isCancelled == true) return@executeOnPooledThread

                listModel = fuzzierUtil.sortAndLimit(listModel)

                if (task?.isCancelled == true) return@executeOnPooledThread

                ApplicationManager.getApplication().invokeLater {
                    component.fileList.model = listModel
                    component.fileList.cellRenderer = getCellRenderer()
                    component.fileList.setPaintBusy(false)
                    if (!component.fileList.isEmpty) {
                        component.fileList.setSelectedValue(listModel[0], true)
                    }
                }
            } catch (_: InterruptedException) {
                return@executeOnPooledThread
            } catch (_: CancellationException) {
                return@executeOnPooledThread
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
        return StringEvaluator(
            projectState.exclusionSet,
            projectState.modules,
            changeListManager
        )
    }

    private fun process(
        project: Project, stringEvaluator: StringEvaluator, searchString: String,
        listModel: DefaultListModel<FuzzyContainer>, task: Future<*>?
    ) {
        val moduleManager = ModuleManager.getInstance(project)
        if (projectState.isProject) {
            processProject(project, stringEvaluator, searchString, listModel, task)
        } else {
            processModules(moduleManager, stringEvaluator, searchString, listModel, task)
        }
    }

    private fun processProject(
        project: Project, stringEvaluator: StringEvaluator,
        searchString: String, listModel: DefaultListModel<FuzzyContainer>, task: Future<*>?
    ) {
        val filesToIterate = ConcurrentHashMap.newKeySet<FuzzierUtil.IterationFile>()
        FuzzierUtil.fileIndexToIterationFile(filesToIterate, ProjectFileIndex.getInstance(project), project.name, task)
        processFiles(filesToIterate, stringEvaluator, listModel, searchString, task)
    }

    private fun processModules(
        moduleManager: ModuleManager, stringEvaluator: StringEvaluator,
        searchString: String, listModel: DefaultListModel<FuzzyContainer>, task: Future<*>?
    ) {
        val filesToIterate = ConcurrentHashMap.newKeySet<FuzzierUtil.IterationFile>()
        for (module in moduleManager.modules) {
            FuzzierUtil.fileIndexToIterationFile(filesToIterate, module.rootManager.fileIndex, module.name, task)
        }
        processFiles(filesToIterate, stringEvaluator, listModel, searchString, task)
    }

    /**
     * Processes a set of IterationFiles concurrently
     */
    private fun processFiles(
        filesToIterate: ConcurrentHashMap.KeySetView<FuzzierUtil.IterationFile, Boolean>,
        stringEvaluator: StringEvaluator, listModel: DefaultListModel<FuzzyContainer>,
        searchString: String, task: Future<*>?
    ) {
        val ss = FuzzierUtil.cleanSearchString(searchString, projectState.ignoredCharacters)
        val processedFiles = ConcurrentHashMap.newKeySet<String>()
        runBlocking {
            withContext(Dispatchers.IO) {
                filesToIterate.forEach { iterationFile ->
                    if (task?.isCancelled == true) return@forEach
                    if (processedFiles.add(iterationFile.file.path)) {
                        launch {
                            stringEvaluator.evaluateFile(iterationFile, listModel, ss)
                        }
                    }
                }
            }
        }
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
            if (!event.valueIsAdjusting) {
                if (component.fileList.isEmpty) {
                    ApplicationManager.getApplication().invokeLater {
                        defaultDoc?.let { (component as FuzzyFinderComponent).previewPane.updateFile(it) }
                    }
                    return@addListSelectionListener
                }
                val selectedValue = component.fileList.selectedValue
                val fileUrl = "file://${selectedValue?.getFileUri()}"

                ProgressManager.getInstance().run(object : Task.Backgroundable(null, "Loading file", false) {
                    override fun run(indicator: ProgressIndicator) {
                        val file = VirtualFileManager.getInstance().findFileByUrl(fileUrl)
                        file?.let {
                            (component as FuzzyFinderComponent).previewPane.updateFile(file)
                        }
                    }
                })
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
}