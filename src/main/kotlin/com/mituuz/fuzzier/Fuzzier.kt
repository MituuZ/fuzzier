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
package com.mituuz.fuzzier

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.impl.EditorHistoryManager
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.rootManager
import com.intellij.openapi.roots.ModuleFileIndex
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.JBPopupListener
import com.intellij.openapi.ui.popup.LightweightWindowEvent
import com.intellij.openapi.util.DimensionService
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.wm.WindowManager
import com.jetbrains.rd.util.ConcurrentHashMap
import com.mituuz.fuzzier.components.FuzzyFinderComponent
import com.mituuz.fuzzier.entities.FuzzyMatchContainer
import com.mituuz.fuzzier.settings.FuzzierSettingsService.RecentFilesMode.NONE
import kotlinx.coroutines.*
import org.apache.commons.lang3.StringUtils
import java.awt.event.*
import javax.swing.*
import kotlin.coroutines.cancellation.CancellationException

open class Fuzzier : FuzzyAction() {
    private var defaultDoc: Document? = null
    open var title: String = "Fuzzy Search"
    private val fuzzyDimensionKey: String = "FuzzySearchPopup"
    // Used by FuzzierVCS to check if files are tracked by the VCS
    protected var changeListManager: ChangeListManager? = null

    override fun runAction(project: Project, actionEvent: AnActionEvent) {
        setCustomHandlers()
        ApplicationManager.getApplication().invokeLater {
            defaultDoc = EditorFactory.getInstance().createDocument("")
            component = FuzzyFinderComponent(project)
            createListeners(project)
            createSharedListeners(project)

            val mainWindow = WindowManager.getInstance().getIdeFrame(actionEvent.project)?.component
            mainWindow?.let {
                popup = createPopup(project)

                if (fuzzierSettingsService.state.resetWindow) {
                    DimensionService.getInstance().setSize(fuzzyDimensionKey, null, project)
                    DimensionService.getInstance().setLocation(fuzzyDimensionKey, null, project)
                    fuzzierSettingsService.state.resetWindow = false
                }
                popup!!.showInCenterOf(it)
                (component as FuzzyFinderComponent).splitPane.dividerLocation =
                    fuzzierSettingsService.state.splitPosition
            }

            if (fuzzierSettingsService.state.recentFilesMode != NONE) {
                createInitialView(project)
            }
        }
    }

    private fun createPopup(project: Project): JBPopup {
        val popup: JBPopup = JBPopupFactory
            .getInstance()
            .createComponentPopupBuilder(component, component.searchField)
            .setFocusable(true)
            .setRequestFocus(true)
            .setResizable(true)
            .setDimensionServiceKey(project, fuzzyDimensionKey, true)
            .setTitle(title)
            .setMovable(true)
            .setShowBorder(true)
            .createPopup()

        popup.addListener(object : JBPopupListener {
            override fun onClosed(event: LightweightWindowEvent) {
                fuzzierSettingsService.state.splitPosition =
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
            val editorHistory = EditorHistoryManager.getInstance(project).fileList
            val listModel = DefaultListModel<FuzzyMatchContainer>()
            val limit = fuzzierSettingsService.state.fileListLimit

            // Start from the end of editor history (most recent file)
            var i = editorHistory.size - 1
            while (i >= 0 && listModel.size() < limit) {
                val file = editorHistory[i]
                val filePathAndModule = fuzzierUtil.removeModulePath(file.path)
                // Don't add files that do not have a module path in the project
                if (filePathAndModule.second == "") {
                    i--
                    continue
                }
                val fuzzyMatchContainer =
                    FuzzyMatchContainer.createOrderedContainer(i, filePathAndModule.first, filePathAndModule.second, file.name)
                listModel.addElement(fuzzyMatchContainer)
                i--
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
            if (fuzzierSettingsService.state.recentFilesMode != NONE) {
                createInitialView(project)
            } else {
                ApplicationManager.getApplication().invokeLater {
                    component.fileList.model = DefaultListModel()
                    defaultDoc?.let { (component as FuzzyFinderComponent).previewPane.updateFile(it) }
                }
            }
            return
        }

        currentTask?.takeIf { !it.isDone }?.cancel(true)
        currentTask = ApplicationManager.getApplication().executeOnPooledThread {
            try {
                // Create a reference to the current task to check if it has been cancelled
                val task = currentTask
                component.fileList.setPaintBusy(true)
                var listModel = DefaultListModel<FuzzyMatchContainer>()

                val stringEvaluator = StringEvaluator(
                    fuzzierSettingsService.state.exclusionSet,
                    fuzzierSettingsService.state.modules,
                    changeListManager,
                    changeListManager?.ignoredFilePaths
                )

                if (task?.isCancelled == true) throw CancellationException()

                val moduleManager = ModuleManager.getInstance(project)
                if (fuzzierSettingsService.state.isProject) {
                    processProject(project, stringEvaluator, searchString, listModel)
                } else {
                    processModules(moduleManager, stringEvaluator, searchString, listModel)
                }

                if (task?.isCancelled == true) throw CancellationException()

                listModel = fuzzierUtil.sortAndLimit(listModel)

                if (task?.isCancelled == true) throw CancellationException()

                ApplicationManager.getApplication().invokeLater {
                    component.fileList.model = listModel
                    component.fileList.cellRenderer = getCellRenderer()
                    component.fileList.setPaintBusy(false)
                    if (!component.fileList.isEmpty) {
                        component.fileList.setSelectedValue(listModel[0], true)
                    }
                }
            } catch (e: CancellationException) {
                // Do nothing
            }
        }
    }
    
    private fun processProject(project: Project, stringEvaluator: StringEvaluator,
                               searchString: String, listModel: DefaultListModel<FuzzyMatchContainer>) {
        val contentIterator = stringEvaluator.getContentIterator(project.name, searchString, listModel)
        ProjectFileIndex.getInstance(project).iterateContent(contentIterator)
    }
    
    data class IterationFile(val file: VirtualFile, val module: String)

    private fun processModules(moduleManager: ModuleManager, stringEvaluator: StringEvaluator,
                               searchString: String, listModel: DefaultListModel<FuzzyMatchContainer>) {
        val filesToIterate = ConcurrentHashMap.newKeySet<IterationFile>()
        for (module in moduleManager.modules) {
            // This could be a method
            val moduleFileIndex: ModuleFileIndex = module.rootManager.fileIndex
            moduleFileIndex.iterateContent { file ->
                if (!file.isDirectory) {
                    filesToIterate.add(IterationFile(file, module.name))
                }
                true
            }
        }
        
        runBlocking {
            withContext(Dispatchers.IO) {
                filesToIterate.forEach { iterationFile ->
                    launch {
                        stringEvaluator.processFile(iterationFile, listModel, searchString)
                    }
                }
            }
        }
    }

    private fun openFile(project: Project, virtualFile: VirtualFile) {
        val fileEditorManager = FileEditorManager.getInstance(project)
        val currentEditor = fileEditorManager.selectedTextEditor
        val previousFile = currentEditor?.virtualFile

        if (fileEditorManager.isFileOpen(virtualFile)) {
            fileEditorManager.openFile(virtualFile, true)
        } else {
            fileEditorManager.openFile(virtualFile, true)
            if (currentEditor != null && !fuzzierSettingsService.state.newTab) {
                fileEditorManager.selectedEditor?.let {
                    if (previousFile != null) {
                        fileEditorManager.closeFile(previousFile)
                    }
                }
            }
        }
        popup?.cancel()
    }

    private fun createListeners(project: Project) {
        // Add a listener that updates the contents of the preview pane
        component.fileList.addListSelectionListener { event ->
            if (!event.valueIsAdjusting) {
                if (component.fileList.isEmpty) {
                    ApplicationManager.getApplication().invokeLater {
                        // This can throw slow operation on ETD (previewPane.updateFile)
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
                        openFile(project, it)
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
                    openFile(project, it)
                }
            }
        })
    }
}