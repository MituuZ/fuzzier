package com.mituuz.fuzzier

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.actionSystem.KeyboardShortcut
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.EditorActionHandler
import com.intellij.openapi.editor.actionSystem.EditorActionManager
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.keymap.KeymapManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopup
import com.mituuz.fuzzier.components.FuzzyComponent
import com.mituuz.fuzzier.entities.FuzzyMatchContainer
import com.mituuz.fuzzier.entities.FuzzyMatchContainer.FilenameType
import com.mituuz.fuzzier.entities.FuzzyMatchContainer.FilenameType.FILE_PATH_ONLY
import com.mituuz.fuzzier.settings.FuzzierSettingsService
import com.mituuz.fuzzier.util.FuzzierUtil
import java.awt.Component
import java.awt.event.ActionEvent
import java.util.*
import java.util.Timer
import java.util.concurrent.Future
import javax.swing.*
import kotlin.concurrent.schedule

abstract class FuzzyAction : AnAction() {
    lateinit var component: FuzzyComponent
    protected var popup: JBPopup? = null
    private lateinit var originalDownHandler: EditorActionHandler
    private lateinit var originalUpHandler: EditorActionHandler
    private var debounceTimer: TimerTask? = null
    protected val fuzzierSettingsService = service<FuzzierSettingsService>()
    @Volatile
    var currentTask: Future<*>? = null
    val fuzzierUtil = FuzzierUtil()

    override fun actionPerformed(actionEvent: AnActionEvent) {
        // Necessary override
    }

    fun createSharedListeners(project: Project) {
        val inputMap = component.searchField.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)

        val keymapManager = KeymapManager.getInstance()
        val activeKeymap = keymapManager.activeKeymap

        val downActionId = "com.mituuz.fuzzier.util.MoveDownAction"
        val upActionId = "com.mituuz.fuzzier.util.MoveUpAction"

        var shortcuts = activeKeymap.getShortcuts(downActionId)

        for (shortcut in shortcuts) {
            if (shortcut is KeyboardShortcut) {
                inputMap.put(shortcut.firstKeyStroke, "moveDown")
            }
        }

        shortcuts = activeKeymap.getShortcuts(upActionId)
        for (shortcut in shortcuts) {
            if (shortcut is KeyboardShortcut) {
                inputMap.put(shortcut.firstKeyStroke, "moveUp")
            }
        }

        component.searchField.actionMap.put("moveUp", object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent?) {
                moveListUp()
            }
        })
        component.searchField.actionMap.put("moveDown", object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent?) {
                moveListDown()
            }
        })

        val document = component.searchField.document
        document.addDocumentListener(object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                debounceTimer?.cancel()
                val debouncePeriod = fuzzierSettingsService.state.debouncePeriod
                debounceTimer = Timer().schedule(debouncePeriod.toLong()) {
                    updateListContents(project, component.searchField.text)
                }
            }
        })
    }

    abstract fun updateListContents(project: Project, searchString: String)

    fun setCustomHandlers() {
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

    class FuzzyListActionHandler(private val fuzzyAction: FuzzyAction, private val isUp: Boolean) : EditorActionHandler() {
        override fun doExecute(editor: Editor, caret: Caret?, dataContext: DataContext?) {
            if (isUp) {
                fuzzyAction.moveListUp()
            } else {
                fuzzyAction.moveListDown()
            }

            super.doExecute(editor, caret, dataContext)
        }
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

    fun getCellRenderer(): ListCellRenderer<Any?> {
        return object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(
                list: JList<*>?,
                value: Any?,
                index: Int,
                isSelected: Boolean,
                cellHasFocus: Boolean
            ): Component {
                val renderer =
                    super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus) as JLabel
                val container = value as FuzzyMatchContainer
                val filenameType: FilenameType = if (component.isDirSelector) {
                    FILE_PATH_ONLY // Directories are always shown as full paths
                } else {
                    fuzzierSettingsService.state.filenameType
                }
                renderer.text = container.toString(filenameType)
                fuzzierSettingsService.state.fileListSpacing.let {
                    renderer.border = BorderFactory.createEmptyBorder(it, 0, it, 0)
                }
                fuzzierSettingsService.state.fontSize.let {
                    renderer.font = renderer.font.deriveFont(it.toFloat())
                }
                return renderer
            }
        }
    }

    // Used for testing
    fun setFiletype(filenameType: FilenameType) {
        fuzzierSettingsService.state.filenameType = filenameType
    }
}