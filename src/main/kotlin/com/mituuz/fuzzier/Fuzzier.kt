package com.mituuz.fuzzier

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.actionSystem.EditorActionHandler
import com.intellij.openapi.editor.actionSystem.EditorActionManager
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ContentIterator
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.JBPopupListener
import com.intellij.openapi.ui.popup.LightweightWindowEvent
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.wm.WindowManager
import com.mituuz.fuzzier.settings.FuzzierSettingsService
import org.apache.commons.lang3.StringUtils
import java.awt.event.*
import javax.swing.*

class Fuzzier : AnAction() {
    private lateinit var component: FuzzyFinder
    private var popup: JBPopup? = null
    private var defaultDoc: Document? = null
    private lateinit var originalDownHandler: EditorActionHandler
    private lateinit var originalUpHandler: EditorActionHandler
    private var fuzzierSettingsService = service<FuzzierSettingsService>()

    override fun actionPerformed(p0: AnActionEvent) {
        setCustomHandlers()
        SwingUtilities.invokeLater {
            defaultDoc = EditorFactory.getInstance().createDocument("")
            p0.project?.let { project ->
                component = FuzzyFinder(project)

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

                    popup?.addListener(object : JBPopupListener {
                        override fun onClosed(event: LightweightWindowEvent) {
                            fuzzierSettingsService.state.splitPosition = component.splitPane.dividerLocation
                            resetOriginalHandlers()
                            super.onClosed(event)
                        }
                    })
                    popup!!.showInCenterOf(it)
                    component.splitPane.dividerLocation = fuzzierSettingsService.state.splitPosition
                }
            }
        }
    }

    private fun setCustomHandlers() {
        val actionManager = EditorActionManager.getInstance()
        originalDownHandler = actionManager.getActionHandler(IdeActions.ACTION_EDITOR_MOVE_CARET_DOWN)
        originalUpHandler = actionManager.getActionHandler(IdeActions.ACTION_EDITOR_MOVE_CARET_UP)

        actionManager.setActionHandler(IdeActions.ACTION_EDITOR_MOVE_CARET_DOWN, FuzzyListActionHandler(this, false))
        actionManager.setActionHandler(IdeActions.ACTION_EDITOR_MOVE_CARET_UP, FuzzyListActionHandler(this, true))
    }

    fun resetOriginalHandlers() {
        val actionManager = EditorActionManager.getInstance()
        actionManager.setActionHandler(IdeActions.ACTION_EDITOR_MOVE_CARET_DOWN, originalDownHandler)
        actionManager.setActionHandler(IdeActions.ACTION_EDITOR_MOVE_CARET_UP, originalUpHandler)
    }

    fun moveListUp() {
        val selectedIndex = component.fileList.selectedIndex
        if (selectedIndex > 0) {
            component.fileList.selectedIndex = selectedIndex - 1
            component.fileList.ensureIndexIsVisible(selectedIndex - 1)
        }
    }

    fun moveListDown() {
        val selectedIndex = component.fileList.selectedIndex
        val length = component.fileList.model.size
        if (selectedIndex < length - 1) {
            component.fileList.selectedIndex = selectedIndex + 1
            component.fileList.ensureIndexIsVisible(selectedIndex + 1)
        }
    }

    fun updateListContents(project: Project, searchString: String) {
        if (StringUtils.isBlank(searchString)) {
            SwingUtilities.invokeLater {
                component.fileList.model = DefaultListModel()
                defaultDoc?.let { component.previewPane.updateFile(it) }
            }
            return
        }

        ApplicationManager.getApplication().executeOnPooledThread {
            component.fileList.setPaintBusy(true)
            val listModel = DefaultListModel<FuzzyMatchContainer>()
            val projectFileIndex = ProjectFileIndex.getInstance(project)
            val projectBasePath = project.basePath

            val contentIterator = projectBasePath?.let { getContentIterator(it, searchString, listModel) }

            if (contentIterator != null) {
                projectFileIndex.iterateContent(contentIterator)
            }
            val sortedList = listModel.elements().toList().sortedByDescending { it.score }
            val valModel = DefaultListModel<String>()
            sortedList.forEach { valModel.addElement(it.string) }

            SwingUtilities.invokeLater {
                component.fileList.model = valModel
                component.fileList.setPaintBusy(false)
                if (!component.fileList.isEmpty) {
                    component.fileList.setSelectedValue(valModel[0], true)
                }
            }
        }
    }

    fun getContentIterator(projectBasePath: String, searchString: String, listModel: DefaultListModel<FuzzyMatchContainer>): ContentIterator {
       return ContentIterator { file: VirtualFile ->
           if (!file.isDirectory) {
               val filePath = projectBasePath.let { it1 -> file.path.removePrefix(it1) }
               val exclusionList = fuzzierSettingsService.state.exclusionList

               if (exclusionList.any { StringUtils.contains(filePath, it) }) {
                   return@ContentIterator true
               }

               if (filePath.isNotBlank()) {
                   val fuzzyMatchContainer = fuzzyContainsCaseInsensitive(filePath, searchString)
                   if (fuzzyMatchContainer != null) {
                       listModel.addElement(fuzzyMatchContainer)
                   }
               }
           }
           true
       }
    }

    fun fuzzyContainsCaseInsensitive(filePath: String, searchString: String): FuzzyMatchContainer? {
        if (searchString.isBlank()) {
            return FuzzyMatchContainer(0, filePath)
        }
        if (searchString.length > filePath.length) {
            return null
        }

        val lowerFilePath: String = filePath.lowercase()
        val lowerSearchString: String = searchString.lowercase()
        return getFuzzyMatch(lowerFilePath, lowerSearchString, filePath)
    }

    private fun getFuzzyMatch(lowerFilePath: String, lowerSearchString: String, filePath: String): FuzzyMatchContainer? {
        var score = 0
        for (s in StringUtils.split(lowerSearchString, " ")) {
            score += processSearchString(s, lowerFilePath) ?: return null
        }
        return FuzzyMatchContainer(score, filePath)
    }

    private fun processSearchString(s: String, lowerFilePath: String): Int? {
        var searchIndex = 0
        var longestStreak = 0
        var streak = 0
        for (i in lowerFilePath.indices) {
            if (lowerFilePath.length - i < s.length - searchIndex) {
                return null
            }

            val char = lowerFilePath[i]
            if (char == s[searchIndex]) {
                streak++
                searchIndex++
                if (searchIndex == s.length) {
                    return calculateScore(streak, longestStreak, lowerFilePath, s)
                }
            } else {
                if (streak > longestStreak) {
                    longestStreak = streak
                }
                streak = 0
            }
        }
        return null
    }

    private fun calculateScore(streak: Int, longestStreak: Int, lowerFilePath: String, lowerSearchString: String): Int {
        var score: Int = if (streak > longestStreak) {
            streak
        } else {
            longestStreak
        }

        StringUtils.split(lowerFilePath, "/.").forEach {
            if (it == lowerSearchString) {
                score += 10
            }
        }

        return score
    }

    data class FuzzyMatchContainer(val score: Int, val string: String)

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
                    ApplicationManager.getApplication().invokeLater {
                        defaultDoc?.let { component.previewPane.updateFile(it) }
                    }
                    return@addListSelectionListener
                }
                val selectedValue = component.fileList.selectedValue
                val fileUrl = "file://$projectBasePath$selectedValue"

                ProgressManager.getInstance().run(object : Task.Backgroundable(null, "Loading file", false) {
                    override fun run(indicator: ProgressIndicator) {
                        val file = VirtualFileManager.getInstance().findFileByUrl(fileUrl)
                        file?.let {
                            component.previewPane.updateFile(file)
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
        val kShiftKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_K, InputEvent.CTRL_DOWN_MASK)
        val jShiftKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_J, InputEvent.CTRL_DOWN_MASK)
        inputMap.put(kShiftKeyStroke, "moveUp")
        component.searchField.actionMap.put("moveUp", object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent?) {
                moveListUp()
            }
        })
        inputMap.put(jShiftKeyStroke, "moveDown")
        component.searchField.actionMap.put("moveDown", object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent?) {
                moveListDown()
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
