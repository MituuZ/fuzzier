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

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.actionSystem.KeyboardShortcut
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.EditorActionHandler
import com.intellij.openapi.editor.actionSystem.EditorActionManager
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.keymap.KeymapManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.DimensionService
import com.intellij.openapi.wm.WindowManager
import com.mituuz.fuzzier.components.FuzzyComponent
import com.mituuz.fuzzier.entities.FuzzyContainer
import com.mituuz.fuzzier.entities.FuzzyContainer.FilenameType
import com.mituuz.fuzzier.settings.FuzzierGlobalSettingsService
import com.mituuz.fuzzier.settings.FuzzierSettingsService
import com.mituuz.fuzzier.util.FuzzierUtil
import com.mituuz.fuzzier.util.FuzzierUtil.Companion.createDimensionKey
import java.awt.Component
import java.awt.event.ActionEvent
import java.util.*
import java.util.Timer
import java.util.concurrent.Future
import javax.swing.*
import kotlin.concurrent.schedule

abstract class FuzzyAction : AnAction() {
    open lateinit var dimensionKey: String
    open lateinit var popupTitle: String
    lateinit var component: FuzzyComponent
    lateinit var popup: JBPopup
    private lateinit var originalDownHandler: EditorActionHandler
    private lateinit var originalUpHandler: EditorActionHandler
    private var debounceTimer: TimerTask? = null
    protected lateinit var projectState: FuzzierSettingsService.State
    protected val globalState = service<FuzzierGlobalSettingsService>().state
    protected var defaultDoc: Document? = null
    @Volatile
    var currentTask: Future<*>? = null
    val fuzzierUtil = FuzzierUtil()

    override fun actionPerformed(actionEvent: AnActionEvent) {
        val project = actionEvent.project
        if (project != null) {
            projectState = project.service<FuzzierSettingsService>().state
            fuzzierUtil.parseModules(project)
            runAction(project, actionEvent)
        }
    }

    abstract fun runAction(project: Project, actionEvent: AnActionEvent)

    abstract fun createPopup(screenDimensionKey: String): JBPopup

    fun getInitialPopup(screenDimensionKey: String): JBPopup {
        return JBPopupFactory
            .getInstance()
            .createComponentPopupBuilder(component, component.searchField)
            .setFocusable(true)
            .setRequestFocus(true)
            .setResizable(true)
            .setDimensionServiceKey(null, screenDimensionKey, true)
            .setTitle(popupTitle)
            .setMovable(true)
            .setShowBorder(true)
            .createPopup()
    }

    fun showPopup(project: Project) {
        val mainWindow = WindowManager.getInstance().getIdeFrame(project)?.component
        mainWindow?.let {
            val screenBounds = it.graphicsConfiguration.bounds
            val screenDimensionKey = createDimensionKey(dimensionKey, screenBounds)

            if (globalState.resetWindow) {
                DimensionService.getInstance().setSize(screenDimensionKey, component.preferredSize, null)
                DimensionService.getInstance().setLocation(screenDimensionKey, null, null)
                globalState.resetWindow = false
            }

            popup = createPopup(screenDimensionKey)
            popup.showInCenterOf(it)
        }
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
                val debouncePeriod = globalState.debouncePeriod
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
                val container = value as FuzzyContainer
                renderer.text = when (component.isDirSelector) {
                    true -> container.getDirDisplayString()
                    false -> container.getDisplayString(globalState)
                }

                globalState.fileListSpacing.let {
                    renderer.border = BorderFactory.createEmptyBorder(it, 0, it, 0)
                }
                globalState.fileListFontSize.let {
                    renderer.font = renderer.font.deriveFont(it.toFloat())
                }
                return renderer
            }
        }
    }

    // Used for testing
    fun setFiletype(filenameType: FilenameType) {
        globalState.filenameType = filenameType
    }

    fun setHighlight(highlight: Boolean) {
        globalState.highlightFilename = highlight
    }
}