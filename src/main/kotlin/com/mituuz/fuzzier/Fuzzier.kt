package com.mituuz.fuzzier

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ContentIterator
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.wm.WindowManager
import org.apache.commons.lang3.StringUtils
import java.awt.event.*
import javax.swing.AbstractAction
import javax.swing.DefaultListModel
import javax.swing.JComponent
import javax.swing.KeyStroke
import javax.swing.SwingUtilities

class Fuzzier : AnAction() {
    private lateinit var component: FuzzyFinder
    private var popup: JBPopup? = null

    override fun actionPerformed(p0: AnActionEvent) {
        SwingUtilities.invokeLater {
            component = FuzzyFinder()
            component.searchField.isEnabled = true
            component.searchField.isVisible = true
            component.searchField.text = ""

            p0.project?.let { project ->
                val projectBasePath = project.basePath
                if (projectBasePath != null) {
                    createListeners(project, projectBasePath)
                }

                val mainWindow = WindowManager.getInstance().getIdeFrame(p0.project)?.component
                mainWindow?.let {
                    popup = JBPopupFactory
                        .getInstance()
                        .createComponentPopupBuilder(component, component.searchField)
                        .setFocusable(true)
                        .setRequestFocus(true)
                        .setResizable(true)
                        .setDimensionServiceKey(project, "FuzzySearchPopup", true)
                        .setTitle("Fuzzy Search")
                        .setMovable(true)
                        .setShowBorder(true)
                        .createPopup()
                    popup!!.showInCenterOf(it)
                }
            }
        }
    }

    fun updateListContents(project: Project, searchString: String) {
        if (StringUtils.isBlank(searchString)) {
            SwingUtilities.invokeLater {
                component.fileList.model = DefaultListModel();
                component.previewPane.text = ""
            }
            return
        }

        component.fileList.setPaintBusy(true)
        val listModel = DefaultListModel<String>()
        val projectFileIndex = ProjectFileIndex.getInstance(project)
        val projectBasePath = project.basePath

        val contentIterator = ContentIterator { file: VirtualFile ->
            if (!file.isDirectory) {
                val filePath = projectBasePath?.let { it1 -> file.path.removePrefix(it1) }
                if (!filePath.isNullOrBlank() && fuzzyContains(filePath, searchString)) {
                    listModel.addElement(filePath)
                }
            }
            true
        }

        projectFileIndex.iterateContent(contentIterator)
        SwingUtilities.invokeLater {
            component.fileList?.model = listModel

            component.fileList.setPaintBusy(false)

            if (!component.fileList.isEmpty) {
                component.fileList.setSelectedValue(listModel[0], true)
            }
        }
    }

    private fun fuzzyContains(filePath: String, searchString: String): Boolean {
        val lowerFilePath: String = filePath.lowercase()
        val lowerSearchString: String = searchString.lowercase()
        return StringUtils.contains(lowerFilePath, lowerSearchString)
    }

    private fun openFile(project: Project, virtualFile: VirtualFile) {
        val fileEditorManager = FileEditorManager.getInstance(project)

        val currentEditor = fileEditorManager.selectedTextEditor

        // Either open the file if there is already a tab for it or close current tab and open the file in a new one
        if (fileEditorManager.isFileOpen(virtualFile)) {
            fileEditorManager.openFile(virtualFile, true)
        } else {
            if (currentEditor != null) {
                fileEditorManager.selectedEditor?.let { fileEditorManager.closeFile(it.file) }
            }
            fileEditorManager.openFile(virtualFile, true)
        }

        popup?.cancel()
    }

    private fun createListeners(project: Project, projectBasePath: String) {
        // Add listener that updates the contents of the preview pane
        component.fileList.addListSelectionListener { event ->
            if (!event.valueIsAdjusting) {
                if (component.fileList.isEmpty) {
                    component.previewPane.text = ""
                    return@addListSelectionListener
                }
                val selectedValue = component.fileList.selectedValue
                val fileUrl = "file://$projectBasePath$selectedValue"

                ProgressManager.getInstance().run(object : Task.Backgroundable(null, "Loading file", false) {
                    override fun run(indicator: ProgressIndicator) {
                        val file = VirtualFileManager.getInstance().findFileByUrl(fileUrl)
                        file?.let {
                            var fileContent = ""
                            var caretPos = 0

                            // Run read action to get document content
                            ApplicationManager.getApplication().runReadAction {
                                val document = FileDocumentManager.getInstance().getDocument(it)
                                fileContent = document?.text ?: "Cannot read file"
                                // caretPos = document?.text?.length?.div(2) ?: 0
                                caretPos = 0
                            }

                            ApplicationManager.getApplication().invokeLater {
                                component.previewPane.text = fileContent
                                component.previewPane.caretPosition = caretPos
                            }
                        }
                    }
                })
            }
        }

        // Add MouseListener for double-click
        component.fileList.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 2) {
                    val selectedValue = component.fileList.selectedValue
                    val virtualFile = VirtualFileManager.getInstance().findFileByUrl("file://$projectBasePath$selectedValue")
                    // Open the file in the editor
                    virtualFile?.let {
                        openFile(project, it)
                    }
                }
            }
        })

        // Add listener that opens the currently selected file when pressing enter (focus on the text box)
        val enterKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0)
        val enterActionKey = "openFile"
        val inputMap = component.searchField.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
        inputMap.put(enterKeyStroke, enterActionKey)
        component.searchField.actionMap.put(enterActionKey, object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent?) {
                val selectedValue = component.fileList.selectedValue
                val virtualFile = VirtualFileManager.getInstance().findFileByUrl("file://$projectBasePath$selectedValue")
                virtualFile?.let {
                    openFile(project, it)
                }
            }
        })

        // Add a listener that updates the search list every time a change is made
        val document = component.searchField.document
        document.addDocumentListener(object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                updateListContents(project, component.searchField.text)
            }
        })
    }
}
