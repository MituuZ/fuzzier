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
import org.apache.commons.lang3.StringUtils
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.AbstractAction
import javax.swing.DefaultListModel
import javax.swing.JComponent
import javax.swing.KeyStroke

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
            val task = currentTask
            component.fileList.setPaintBusy(true)
            val listModel = DefaultListModel<FuzzyContainer>()

            if (task?.isCancelled == true) return@executeOnPooledThread

            findInFiles(searchString, listModel, project)

            if (task?.isCancelled == true) return@executeOnPooledThread

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

    private fun findInFiles(searchString: String, listModel: DefaultListModel<FuzzyContainer>, project: Project) {
        val fileIndex = ProjectFileIndex.getInstance(project)
        val projectBasePath = project.basePath.toString()

        fileIndex.iterateContent {
            if (currentTask?.isCancelled == true) {
                return@iterateContent false
            }
            ApplicationManager.getApplication().runReadAction {
                it.findDocument()?.text?.let { text ->
                    var found = false
                    var filePath = ""

                    var rows = text.split("\n")
                    var rowCount = rows.size
                    var i = 0

                    while (i < rowCount) {
                        val row = rows[i]
                        if (row.contains(searchString, ignoreCase = true)) {
                            if (found == false) {
                                // Setup file info
                                filePath = it.path.removePrefix(projectBasePath)
                                found = true
                            }
                            val columnNumber = row.indexOf(searchString, ignoreCase = true)
                            listModel.addElement(RowContainer(filePath, projectBasePath, it.name, i, columnNumber, row.trim()))
                        }

                        i++
                    }
                }
            }
            true
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