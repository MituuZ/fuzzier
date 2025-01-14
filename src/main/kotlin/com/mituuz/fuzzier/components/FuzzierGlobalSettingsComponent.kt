package com.mituuz.fuzzier.components

import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.FormBuilder
import javax.swing.JPanel

class FuzzierGlobalSettingsComponent {
    var jPanel: JPanel

    init {
        jPanel = FormBuilder.createFormBuilder()
            .addComponent(JBLabel("<html><strong>General settings</strong></html>"))
            .panel
    }
}