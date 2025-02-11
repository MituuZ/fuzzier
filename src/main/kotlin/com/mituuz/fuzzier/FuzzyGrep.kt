package com.mituuz.fuzzier

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupListener
import com.intellij.openapi.ui.popup.LightweightWindowEvent
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.findDocument
import com.mituuz.fuzzier.components.FuzzyFinderComponent
import com.mituuz.fuzzier.entities.FuzzyContainer
import com.mituuz.fuzzier.entities.RowContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.apache.commons.lang3.StringUtils
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.AbstractAction
import javax.swing.DefaultListModel
import javax.swing.JComponent
import javax.swing.KeyStroke
import kotlin.coroutines.cancellation.CancellationException

class FuzzyGrep() : FuzzyAction() {
    override var popupTitle: String = "Fuzzy Grep"
    override var dimensionKey = "FuzzyGrepPopup"

    override fun runAction(
        project: Project,
        actionEvent: AnActionEvent
    ) {
        setCustomHandlers()

        ApplicationManager.getApplication().invokeLater {
            defaultDoc = EditorFactory.getInstance().createDocument("")
            component = FuzzyFinderComponent(project)
            createListeners(project)
            createSharedListeners(project)

            showPopup(project)

            (component as FuzzyFinderComponent).splitPane.dividerLocation =
                globalState.splitPosition
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

    override fun updateListContents(project: Project, searchString: String) {
        if (StringUtils.isBlank(searchString)) {
            return
        }

        currentTask?.takeIf { !it.isDone }?.cancel(true)
        currentTask = ApplicationManager.getApplication().executeOnPooledThread {
            try {
                val task = currentTask
                component.fileList.setPaintBusy(true)
                val listModel = DefaultListModel<FuzzyContainer>()
                component.fileList.model = listModel
                component.fileList.cellRenderer = getCellRenderer()

                if (task?.isCancelled == true) return@executeOnPooledThread

                var files = getFiles(project)

                if (task?.isCancelled == true) return@executeOnPooledThread

                findInFiles(searchString, listModel, files, project.basePath.toString(), task)

                if (task?.isCancelled == true) return@executeOnPooledThread

                ApplicationManager.getApplication().invokeLater {
                    val selectedIndex = component.fileList.selectedIndex
                    component.fileList.setPaintBusy(false)

                    // Retain the current selection
                    if (selectedIndex >= 0 && selectedIndex < listModel.size) {
                        component.fileList.selectedIndex = selectedIndex
                    }
                }
            } catch (_: InterruptedException) {
                return@executeOnPooledThread
            } catch (_: CancellationException) {
                return@executeOnPooledThread
            }
        }
    }

    private fun getFiles(project: Project): Set<VirtualFile> {
        val res = HashSet<VirtualFile>()
        val fileIndex = ProjectFileIndex.getInstance(project)
        fileIndex.iterateContent {
            if (currentTask?.isCancelled == true) {
                return@iterateContent false
            }

            res.add(it)
            true
        }

        return res
    }

    private fun findInFiles(searchString: String, listModel: DefaultListModel<FuzzyContainer>,
                                    files: Set<VirtualFile>, projectBasePath: String, task: Future<*>?) {
        val limitReached = AtomicBoolean(false)

        runBlocking {
            withContext(Dispatchers.IO) {
                files.forEach { virtualFile ->
                    if (task?.isCancelled == true || limitReached.get()) return@forEach

                    launch() {
                        ApplicationManager.getApplication().runReadAction {
                            virtualFile.findDocument()?.text?.let { text ->
                                var found = false
                                var filePath = ""

                                var rows = text.split("\n")
                                var rowCount = rows.size
                                var i = 0

                                while (i < rowCount) {
                                    if (task?.isCancelled == true || limitReached.get()) return@runReadAction
                                    val row = rows[i]
                                    if (row.contains(searchString, ignoreCase = true)) {
                                        if (!found) {
                                            // Setup file info
                                            filePath = virtualFile.path.removePrefix(projectBasePath)
                                        }
                                        val columnNumber = row.indexOf(searchString, ignoreCase = true)
                                        listModel.addElement(
                                            RowContainer(
                                                filePath,
                                                projectBasePath,
                                                virtualFile.name,
                                                i,
                                                columnNumber,
                                                row.trim()
                                            )
                                        )

                                        if (!found) {
                                            // Update the preview pane with the first result
                                            if (!component.fileList.isEmpty) {
                                                component.fileList.setSelectedValue(listModel[0], true)
                                            }
                                            found = true
                                        }

                                        if (listModel.size >= globalState.fileListLimit) {
                                            limitReached.set(true)
                                            return@runReadAction
                                        }
                                    }

                                    i++
                                }
                            }
                        }
                    }
                }
            }
        }
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
                            (component as FuzzyFinderComponent).previewPane.updateFile(file, (selectedValue as RowContainer).rowNumber)
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
        popup.cancel()
        ApplicationManager.getApplication().invokeLater {
            val rc = fuzzyContainer as RowContainer
            val lp = LogicalPosition(rc.rowNumber, rc.columnNumber)
            val editor = fileEditorManager.selectedTextEditor
            editor?.scrollingModel?.scrollTo(lp, ScrollType.CENTER)
            editor?.caretModel?.moveToLogicalPosition(lp)
        }
    }
}