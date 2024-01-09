package com.mituuz.fuzzier.settings

import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.FormBuilder
import javax.swing.JPanel

class FuzzierSettingsComponent {
    var jPanel: JPanel
    var exclusionList = JBTextArea()
    private var exclusionInstructions = JBLabel("<html><strong>File path exclusions:</strong><br>" +
            "One line per one exclusion from the Fuzzier results. Empty lines are skipped<br>" +
            "Only supports contains for now. e.g. \"kt\" excludes all files/file paths that contain \"kt\" string. " +
            "(main.kt, ktlin.java)<html>")

    init {
        jPanel = FormBuilder.createFormBuilder()
            .addComponent(exclusionInstructions)
            .addComponent(exclusionList)
            .addComponentFillVertically(JPanel(), 0)
            .panel;
    }
}