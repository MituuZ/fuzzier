package com.mituuz.fuzzier.settings

import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.FormBuilder
import javax.swing.JPanel

class FuzzierSettingsComponent {
    var jPanel: JPanel
    var exclusionList = JBTextArea()
    private var exclusionInstructions = JBLabel("<html><strong>File path exclusions:</strong><br>" +
            "One line per one exclusion from the Fuzzier results.<br>" +
            "Empty lines are skipped and all files in the project root start with \"/\"<br>" +
            "Supports wildcards (*) for starts and ends with. Defaults to contains if no wildcards are present.<br>" +
            "e.g. \"kt\" excludes all files/file paths that contain the \"kt\" string. (main.<strong>kt</strong>, <strong>kt</strong>lin.java)<html>")

    init {
        jPanel = FormBuilder.createFormBuilder()
            .addComponent(exclusionInstructions)
            .addComponent(exclusionList)
            .addComponentFillVertically(JPanel(), 0)
            .panel;
    }
}