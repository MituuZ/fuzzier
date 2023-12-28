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
import javax.swing.*

class Fuzzier : AnAction() {
    private lateinit var component: FuzzyFinder
    private var popup: JBPopup? = null

    override fun actionPerformed(p0: AnActionEvent) {
        SwingUtilities.invokeLater {
            component = FuzzyFinder()
            component.searchField.isEnabled = true
            component.searchField.isVisible = true
            component.searchField.text = ""
            // ToDo: Quick fix
            component.splitPane.setDividerLocation(500)

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
                component.fileList.model = DefaultListModel()
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

                // ToDo: This can be handled better and made configurable
                if (StringUtils.contains(file.path, ".git/")
                    || StringUtils.contains(file.path, ".idea/")
                    || StringUtils.contains(file.path, "build/")) {
                    // Skip these files

                } else if (!filePath.isNullOrBlank() && fuzzyContains(filePath, searchString)) {
                    listModel.addElement(filePath)
                }
            }
            true
        }

        projectFileIndex.iterateContent(contentIterator)
        sortListModel(listModel, searchString)
        SwingUtilities.invokeLater {
            component.fileList?.model = listModel

            component.fileList.setPaintBusy(false)

            if (!component.fileList.isEmpty) {
                component.fileList.setSelectedValue(listModel[0], true)
            }
        }
    }

    private fun sortListModel(model: DefaultListModel<String>, searchString: String) {
        val elements = mutableListOf<String>()
        for (i in 0 until model.size()) {
            model.getElementAt(i)?.let { elements.add(it) }
        }

        val comparator = Comparator<String> { o1, o2 ->
            val count1 = countSequentialChars(o1, searchString)
            val count2 = countSequentialChars(o2, searchString)
            when {
                count1 > count2 -> 1
                count2 > count1 -> -1
                else -> o1.compareTo(o2) // Default comparison if both have or don't have the searchString
            }
        }

        elements.sortWith(comparator)
        model.clear()
        elements.forEach { model.addElement(it) }
    }

    private fun countSequentialChars(filePath: String, searchString: String): Int {
        var count = 0
        var searchIndex = 0
        for (char in filePath) {
            if (char == searchString[searchIndex]) {
                count++
                searchIndex++
                if (searchIndex == searchString.length) break
            }
        }

        return count
    }

    private fun fuzzyContains(filePath: String, searchString: String): Boolean {
        val lowerFilePath: String = filePath.lowercase()
        val lowerSearchString: String = searchString.lowercase()

        var searchPos = 0

        for (char in lowerFilePath) {
            if (searchPos < lowerSearchString.length && char == lowerSearchString[searchPos]) {
                searchPos++
            }
        }

        return searchPos == searchString.length
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
        // Add a listener that updates the contents of the preview pane
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

        // Add a mouse listener for double-click
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

        // Add a listener that opens the currently selected file when pressing enter (focus on the text box)
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

        // Add a listener to move fileList up and down by using CTRL + k/j
        val upKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_K, InputEvent.CTRL_DOWN_MASK)
        val downKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_J, InputEvent.CTRL_DOWN_MASK)
        inputMap.put(upKeyStroke, "moveUp")
        component.searchField.actionMap.put("moveUp", object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent?) {
                val selectedIndex = component.fileList.selectedIndex
                if (selectedIndex > 0) {
                    component.fileList.selectedIndex = selectedIndex - 1
                    component.fileList.ensureIndexIsVisible(selectedIndex - 1)
                }
            }
        })
        inputMap.put(downKeyStroke, "moveDown")
        component.searchField.actionMap.put("moveDown", object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent?) {
                val selectedIndex = component.fileList.selectedIndex
                val length = component.fileList.model.size
                if (selectedIndex < length - 1) {
                    component.fileList.selectedIndex = selectedIndex + 1
                    component.fileList.ensureIndexIsVisible(selectedIndex + 1)
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
