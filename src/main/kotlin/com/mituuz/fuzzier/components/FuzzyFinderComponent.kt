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
package com.mituuz.fuzzier.components

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CustomShortcutSet
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.project.Project
import com.intellij.ui.EditorTextField
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.uiDesigner.core.GridConstraints
import com.intellij.uiDesigner.core.GridLayoutManager
import com.intellij.util.ui.JBUI
import com.mituuz.fuzzier.entities.FuzzyContainer
import com.mituuz.fuzzier.settings.FuzzierGlobalSettingsService
import com.mituuz.fuzzier.settings.FuzzierGlobalSettingsService.SearchPosition.*
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.KeyboardFocusManager
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import javax.swing.JPanel
import javax.swing.KeyStroke
import javax.swing.SwingUtilities

class FuzzyFinderComponent(project: Project, private val showSecondaryField: Boolean = false) : FuzzyComponent() {
    var previewPane: PreviewEditor = PreviewEditor(project)
    var fuzzyPanel: JPanel = JPanel()
    var splitPane: OnePixelSplitter = OnePixelSplitter()
    private val secondaryField = EditorTextField().apply {
        setPlaceholder("file extension")
    }

    init {
        val settingsState = service<FuzzierGlobalSettingsService>().state

        layout = BorderLayout()
        add(fuzzyPanel)
        previewPane.fileType = PlainTextFileType.INSTANCE
        previewPane.isViewer = true

        splitPane.setAndLoadSplitterProportionKey("Fuzzier.FuzzyFinder.Splitter")
        splitPane.preferredSize = Dimension(settingsState.defaultPopupWidth, settingsState.defaultPopupHeight)

        fuzzyPanel.layout = GridLayoutManager(1, 1, JBUI.emptyInsets(), -1, -1)
        val searchPanel = JPanel()
        val cols = if (showSecondaryField) 2 else 1
        searchPanel.layout = GridLayoutManager(3, cols, JBUI.emptyInsets(), -1, -1)

        // Configure the secondary field to be roughly a single word wide
        run {
            val width = JBUI.scale(90)
            secondaryField.preferredSize = Dimension(width, secondaryField.preferredSize.height)
        }
        searchField.text = ""
        val fileListScrollPane = JBScrollPane()
        fileList = JBList<FuzzyContainer>()
        fileList.selectionMode = 0
        fileListScrollPane.setViewportView(fileList)

        // Use JetBrains native splitter divider width
        splitPane.dividerWidth = 10

        when (val searchPosition = settingsState.searchPosition) {
            BOTTOM, TOP -> vertical(searchPosition, searchPanel, fileListScrollPane)
            RIGHT, LEFT -> horizontal(searchPosition, searchPanel, fileListScrollPane)
        }

        if (showSecondaryField) {
            setupTabBetweenFields()
        }
        setupCtrlDUShortcuts()
    }

    private fun setupCtrlDUShortcuts() {
        fun movePreviewHalfPage(down: Boolean) {
            val editor = previewPane.editor ?: return
            val scrollingModel = editor.scrollingModel
            val visible = scrollingModel.visibleArea
            val lineHeight = editor.lineHeight
            if (lineHeight <= 0) return
            val halfPagePx = (visible.height / 2).coerceAtLeast(lineHeight)
            val newY = (visible.y + if (down) halfPagePx else -halfPagePx).coerceAtLeast(0)
            scrollingModel.scrollVertically(newY)
        }

        fun registerCtrlKey(field: EditorTextField, keyCode: Int, down: Boolean) {
            field.addSettingsProvider { editorEx ->
                val action = object : AnAction() {
                    override fun actionPerformed(e: AnActionEvent) {
                        movePreviewHalfPage(down)
                    }
                }
                val ks = KeyStroke.getKeyStroke(keyCode, InputEvent.CTRL_DOWN_MASK)
                action.registerCustomShortcutSet(CustomShortcutSet(ks), editorEx.contentComponent)
            }
        }
        // Main field
        registerCtrlKey(searchField, KeyEvent.VK_D, true)
        registerCtrlKey(searchField, KeyEvent.VK_U, false)
        // Secondary field, when shown
        if (showSecondaryField) {
            registerCtrlKey(secondaryField, KeyEvent.VK_D, true)
            registerCtrlKey(secondaryField, KeyEvent.VK_U, false)
        }
    }

    private fun setupTabBetweenFields() {
        fun isBothShowing(): Boolean = searchField.isShowing && secondaryField.isShowing

        fun registerTabSwitch(forward: Boolean, field: EditorTextField, other: EditorTextField) {
            field.addSettingsProvider { editorEx ->
                val action = object : AnAction() {
                    override fun actionPerformed(e: AnActionEvent) {
                        if (!isBothShowing()) return
                        val focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().focusOwner
                        val onThis = SwingUtilities.isDescendingFrom(focusOwner, field)
                        val onOther = SwingUtilities.isDescendingFrom(focusOwner, other)
                        if (onThis) {
                            other.requestFocusInWindow()
                        } else if (onOther) {
                            field.requestFocusInWindow()
                        } else {
                            // If the focus is elsewhere, do nothing
                        }
                    }
                }
                val ks = if (forward) KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0)
                else KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.SHIFT_DOWN_MASK)
                action.registerCustomShortcutSet(CustomShortcutSet(ks), editorEx.contentComponent)
            }
        }

        // Register for both fields: forward and backward
        registerTabSwitch(true, searchField, secondaryField)
        registerTabSwitch(false, searchField, secondaryField)
        registerTabSwitch(true, secondaryField, searchField)
        registerTabSwitch(false, secondaryField, searchField)
    }

    fun vertical(
        searchPosition: FuzzierGlobalSettingsService.SearchPosition, searchPanel: JPanel,
        fileListScrollPane: JBScrollPane
    ) {
        fuzzyPanel.add(
            splitPane,
            getGridConstraints(0)
        )

        // Vertical orientation: first = top, second = bottom
        splitPane.orientation = true

        var searchFieldGridRow: Int
        var fileListGridRow: Int
        if (searchPosition == TOP) {
            searchFieldGridRow = 0
            fileListGridRow = 1
            splitPane.firstComponent = searchPanel
            splitPane.secondComponent = previewPane
        } else {
            searchFieldGridRow = 1
            fileListGridRow = 0
            splitPane.firstComponent = previewPane
            splitPane.secondComponent = searchPanel
        }

        searchPanel.add(
            searchField,
            getGridConstraints(searchFieldGridRow, 0, 1, true)
        )
        if (showSecondaryField) {
            searchPanel.add(
                secondaryField,
                getSecondaryConstraints(searchFieldGridRow)
            )
        }
        val colSpan = if (showSecondaryField) 2 else 1
        searchPanel.add(
            fileListScrollPane,
            getGridConstraints(fileListGridRow, 0, colSpan)
        )
    }

    fun horizontal(
        searchPosition: FuzzierGlobalSettingsService.SearchPosition, searchPanel: JPanel,
        fileListScrollPane: JBScrollPane
    ) {
        fuzzyPanel.add(
            splitPane,
            getGridConstraints(0)
        )

        searchPanel.add(
            searchField,
            getGridConstraints(1, 0, 1, true)
        )
        if (showSecondaryField) {
            searchPanel.add(
                secondaryField,
                getSecondaryConstraints(1)
            )
        }
        val colSpan = if (showSecondaryField) 2 else 1
        searchPanel.add(
            fileListScrollPane,
            getGridConstraints(0, 0, colSpan)
        )

        // Horizontal orientation: first = left, second = right
        splitPane.orientation = false
        if (searchPosition == LEFT) {
            splitPane.firstComponent = searchPanel
            splitPane.secondComponent = previewPane
        } else {
            splitPane.secondComponent = searchPanel
            splitPane.firstComponent = previewPane
        }
    }

    private fun getGridConstraints(row: Int, sizePolicyFixed: Boolean = false): GridConstraints {
        val sizePolicy = if (sizePolicyFixed) GridConstraints.SIZEPOLICY_FIXED
        else GridConstraints.SIZEPOLICY_CAN_SHRINK or GridConstraints.SIZEPOLICY_WANT_GROW

        return GridConstraints(
            row,
            0,
            1,
            1,
            GridConstraints.ANCHOR_CENTER,
            GridConstraints.FILL_BOTH,
            GridConstraints.SIZEPOLICY_CAN_SHRINK or GridConstraints.SIZEPOLICY_WANT_GROW,
            sizePolicy,
            null,
            Dimension(-1, -1),
            null,
            0,
            false
        )
    }

    private fun getGridConstraints(
        row: Int,
        column: Int,
        colSpan: Int,
        sizePolicyFixed: Boolean = false
    ): GridConstraints {
        val sizePolicy = if (sizePolicyFixed) GridConstraints.SIZEPOLICY_FIXED
        else GridConstraints.SIZEPOLICY_CAN_SHRINK or GridConstraints.SIZEPOLICY_WANT_GROW
        return GridConstraints(
            row,
            column,
            1,
            colSpan,
            GridConstraints.ANCHOR_CENTER,
            GridConstraints.FILL_BOTH,
            GridConstraints.SIZEPOLICY_CAN_SHRINK or GridConstraints.SIZEPOLICY_WANT_GROW,
            sizePolicy,
            null,
            Dimension(-1, -1),
            null,
            0,
            false
        )
    }

    private fun getSecondaryConstraints(row: Int): GridConstraints {
        return GridConstraints(
            row,
            1,
            1,
            1,
            GridConstraints.ANCHOR_CENTER,
            GridConstraints.FILL_HORIZONTAL,
            GridConstraints.SIZEPOLICY_FIXED,
            GridConstraints.SIZEPOLICY_FIXED,
            null,
            Dimension(-1, -1),
            null,
            0,
            false
        )
    }

    fun getSecondaryText(): String = secondaryField.text

    fun getSecondaryField(): EditorTextField = secondaryField

    fun addSecondaryDocumentListener(listener: DocumentListener, parentDisposable: Disposable) {
        secondaryField.document.addDocumentListener(listener, parentDisposable)
    }
}
