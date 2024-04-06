package com.mituuz.fuzzier.components

import com.intellij.ui.components.JBScrollPane
import com.intellij.uiDesigner.core.GridConstraints
import com.intellij.uiDesigner.core.GridLayoutManager
import java.awt.Dimension

class SimpleFinderComponent() : FuzzyComponent() {
    init {
        layout = GridLayoutManager(2, 1)
        val scrollPane = JBScrollPane()
        scrollPane.setViewportView(fileList)

        add(
            scrollPane,
            GridConstraints(
                0,
                0,
                1,
                1,
                GridConstraints.ANCHOR_CENTER,
                GridConstraints.FILL_BOTH,
                GridConstraints.SIZEPOLICY_CAN_SHRINK or GridConstraints.SIZEPOLICY_WANT_GROW,
                GridConstraints.SIZEPOLICY_CAN_SHRINK or GridConstraints.SIZEPOLICY_WANT_GROW,
                Dimension(-1, 300),
                Dimension(-1, -1),
                null,
                0,
                false
            )
        )
        add(
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
    }
}