package com.mituuz.fuzzier.components

import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.project.Project
import com.intellij.ui.EditorTextField
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.uiDesigner.core.GridConstraints
import com.intellij.uiDesigner.core.GridLayoutManager
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JPanel
import javax.swing.JSplitPane

class FuzzyFinderComponent(project: Project) : JPanel(
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

    init {
        layout = BorderLayout()
        add(fuzzyPanel)
        previewPane.fileType = PlainTextFileType.INSTANCE
        previewPane.isViewer = true

        splitPane.preferredSize = Dimension(700, 400)
        splitPane.rightComponent = previewPane

        fuzzyPanel.layout = GridLayoutManager(1, 1, JBUI.emptyInsets(), -1, -1)
        val panel1 = JPanel()
        panel1.layout = GridLayoutManager(2, 1, JBUI.emptyInsets(), -1, -1)
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
                Dimension(-1, -1),
                null,
                0,
                false
            )
        )
        val scrollPane1 = JBScrollPane()

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
                Dimension(-1, -1),
                null,
                0,
                false
            )
        )

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
                Dimension(-1, -1),
                null,
                0,
                false
            )
        )
        fileList = JBList<String?>()
        fileList.selectionMode = 0
        scrollPane1.setViewportView(fileList)
    }
}
