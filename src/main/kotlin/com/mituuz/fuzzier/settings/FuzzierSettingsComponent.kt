package com.mituuz.fuzzier.settings

import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.FormBuilder
import javax.swing.JPanel

class FuzzierSettingsComponent {
    var jPanel: JPanel
    var exclusionList = JBTextArea()

    init {
        jPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel("File path exclusions:"), exclusionList, 1, true)
            .addComponentFillVertically(JPanel(), 0)
            .panel;
    }
}