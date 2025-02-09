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
package com.mituuz.fuzzier.components

import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.components.service
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.uiDesigner.core.GridConstraints
import com.intellij.uiDesigner.core.GridLayoutManager
import com.intellij.util.ui.JBUI
import com.mituuz.fuzzier.entities.FuzzyContainer
import com.mituuz.fuzzier.settings.FuzzierGlobalSettingsService
import com.mituuz.fuzzier.settings.FuzzierGlobalSettingsService.SearchPosition.BOTTOM
import com.mituuz.fuzzier.settings.FuzzierGlobalSettingsService.SearchPosition.LEFT
import com.mituuz.fuzzier.settings.FuzzierGlobalSettingsService.SearchPosition.RIGHT
import com.mituuz.fuzzier.settings.FuzzierGlobalSettingsService.SearchPosition.TOP
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JPanel
import javax.swing.JSplitPane

class FuzzyFinderComponent(project: Project) : FuzzyComponent() {
    var previewPane: PreviewEditor = PreviewEditor(project)
    var fuzzyPanel: JPanel = JPanel()
    var splitPane: JSplitPane = JSplitPane()

    init {
        val settingsState = service<FuzzierGlobalSettingsService>().state

        layout = BorderLayout()
        add(fuzzyPanel)
        previewPane.fileType = PlainTextFileType.INSTANCE
        previewPane.isViewer = true

        splitPane.preferredSize = Dimension(settingsState.defaultPopupWidth, settingsState.defaultPopupHeight)

        fuzzyPanel.layout = GridLayoutManager(1, 1, JBUI.emptyInsets(), -1, -1)
        val searchPanel = JPanel()
        searchPanel.layout = GridLayoutManager(3, 1, JBUI.emptyInsets(), -1, -1)
        searchField.text = ""
        val fileListScrollPane = JBScrollPane()
        fileList = JBList<FuzzyContainer?>()
        fileList.selectionMode = 0
        fileListScrollPane.setViewportView(fileList)

        splitPane.dividerSize = 10

        val searchPosition = settingsState.searchPosition

        when (searchPosition) {
            BOTTOM, TOP -> vertical(searchPosition, searchPanel, fileListScrollPane)
            RIGHT, LEFT -> horizontal(searchPosition, searchPanel, fileListScrollPane)
        }
    }

    fun vertical(searchPosition: FuzzierGlobalSettingsService.SearchPosition, searchPanel: JPanel,
                 fileListScrollPane: JBScrollPane) {
        fuzzyPanel.add(
            splitPane,
            getGridConstraints(0)
        )

        splitPane.orientation = JSplitPane.VERTICAL_SPLIT

        var searchFieldGridRow: Int
        var fileListGridRow: Int
        if (searchPosition == TOP) {
            searchFieldGridRow = 0
            fileListGridRow = 1
            splitPane.topComponent = searchPanel
            splitPane.bottomComponent = previewPane
        } else {
            searchFieldGridRow = 1
            fileListGridRow = 0
            splitPane.topComponent = previewPane
            splitPane.bottomComponent = searchPanel
        }
        searchPanel.add(
            searchField,
            getGridConstraints(searchFieldGridRow, true)
        )
        searchPanel.add(
            fileListScrollPane,
            getGridConstraints(fileListGridRow)
        )
    }

    fun horizontal(searchPosition: FuzzierGlobalSettingsService.SearchPosition, searchPanel: JPanel,
                   fileListScrollPane: JBScrollPane) {
        fuzzyPanel.add(
            splitPane,
            getGridConstraints(0)
        )

        searchPanel.add(
            searchField,
            getGridConstraints(1, true)
        )

        searchPanel.add(
            fileListScrollPane,
            getGridConstraints(0)
        )

        if (searchPosition == LEFT) {
            splitPane.leftComponent = searchPanel
            splitPane.rightComponent = previewPane
        } else {
            splitPane.rightComponent = searchPanel
            splitPane.leftComponent = previewPane
        }
    }

    private fun getGridConstraints(row: Int, sizePolicyFixed: Boolean = false) : GridConstraints {
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
}
