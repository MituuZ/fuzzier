package com.mituuz.fuzzier

import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.project.Project
import com.intellij.ui.EditorTextField
import com.intellij.ui.components.JBList
import com.intellij.uiDesigner.core.GridConstraints
import com.intellij.uiDesigner.core.GridLayoutManager
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Insets
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JSplitPane

class FuzzyFinder(project: Project) : JPanel(
) {
    var previewPane: PreviewEditor = PreviewEditor(project)
        private set
    var fileList: JBList<String?> = JBList<String?>()
        private set
    var searchField: EditorTextField = EditorTextField()
        private set
    private var fuzzyPanel: JPanel = JPanel()
    var splitPane: JSplitPane = JSplitPane()
        private set

    fun createPreviewPane(project: Project?): FuzzyFinder {
        this.previewPane = PreviewEditor(project)
        previewPane.fileType = PlainTextFileType.INSTANCE
        previewPane.isViewer = true
        splitPane.rightComponent = previewPane
        return this
    }

    private fun createUIComponents() {
        this.layout = BorderLayout()
        this.add(fuzzyPanel)
    }

    init {
        setUp()
    }

    private fun setUp() {
        createUIComponents()
        fuzzyPanel.layout = GridLayoutManager(1, 1, Insets(0, 0, 0, 0), -1, -1)
        splitPane.dividerLocation = 300
        splitPane.dividerSize = 10
        fuzzyPanel.add(
            splitPane,
            GridConstraints(
                0,
                0,
                1,
                1,
                GridConstraints.ANCHOR_CENTER,
                GridConstraints.FILL_BOTH,
                GridConstraints.SIZEPOLICY_CAN_SHRINK or GridConstraints.SIZEPOLICY_CAN_GROW,
                GridConstraints.SIZEPOLICY_CAN_SHRINK or GridConstraints.SIZEPOLICY_CAN_GROW,
                null,
                Dimension(200, 200),
                null,
                0,
                false
            )
        )
        val panel1 = JPanel()
        panel1.layout = GridLayoutManager(2, 1, Insets(0, 0, 0, 0), -1, -1)
        splitPane.leftComponent = panel1
        searchField.text = ""
        panel1.add(
            searchField,
            GridConstraints(
                1,
                0,
                1,
                1,
                GridConstraints.ANCHOR_WEST,
                GridConstraints.FILL_HORIZONTAL,
                GridConstraints.SIZEPOLICY_WANT_GROW,
                GridConstraints.SIZEPOLICY_FIXED,
                null,
                Dimension(150, -1),
                null,
                0,
                false
            )
        )
        val scrollPane1 = JScrollPane()
        panel1.add(
            scrollPane1,
            GridConstraints(
                0,
                0,
                1,
                1,
                GridConstraints.ANCHOR_CENTER,
                GridConstraints.FILL_BOTH,
                GridConstraints.SIZEPOLICY_CAN_SHRINK or GridConstraints.SIZEPOLICY_WANT_GROW,
                GridConstraints.SIZEPOLICY_CAN_SHRINK or GridConstraints.SIZEPOLICY_WANT_GROW,
                null,
                Dimension(150, -1),
                null,
                0,
                false
            )
        )
        fileList = JBList<String?>()
        fileList.selectionMode = 0
        scrollPane1.setViewportView(fileList)
        splitPane.rightComponent = previewPane
    }
}
