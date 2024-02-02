package com.mituuz.fuzzier.settings

import com.intellij.icons.AllIcons
import com.intellij.openapi.components.service
import com.intellij.ui.JBColor
import com.intellij.ui.JBIntSpinner
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.FormBuilder
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.border.LineBorder

class FuzzierSettingsComponent {
    var jPanel: JPanel
    var exclusionList = JBTextArea()
    var newTabSelect = JBCheckBox()
    private var exclusionInstructions = JBLabel("<html><strong>File path exclusions:</strong><br>", AllIcons.General.ContextHelp, JBLabel.LEFT)
    var debounceTimerValue = JBIntSpinner(150, 0, 2000)
    private var resetWindowDimension = JButton("Reset popup location")
    var multiMatchActive = JBCheckBox()

    init {
        exclusionList.border = LineBorder(JBColor.BLACK, 1)
        resetWindowDimension.addActionListener {
            service<FuzzierSettingsService>().state.resetWindow = true
        }
        exclusionInstructions.toolTipText =
            "One line per one exclusion from the Fuzzier results.<br><br>" +
                    "Empty lines are skipped and all files in the project root start with \"/\"<br><br>" +
                    "Supports wildcards (*) for starts with and ends with. Defaults to contains if no wildcards are present.<br><br>" +
                    "e.g. \"kt\" excludes all files/file paths that contain the \"kt\" string. (main.<strong>kt</strong>, <strong>kt</strong>lin.java)<html>"

        jPanel = FormBuilder.createFormBuilder()
            .addComponent(exclusionInstructions)
            .addComponent(exclusionList)
            .addSeparator()
            .addLabeledComponent("<html><strong>Open files in a new tab</strong></html>", newTabSelect)
            .addLabeledComponent("<html><strong>Debounce period</strong></html>", debounceTimerValue)
            .addSeparator()
            .addLabeledComponent("<html><strong>Match characters multiple times</strong></html>", multiMatchActive)
            .addComponentFillVertically(JPanel(), 0)
            .addComponent(resetWindowDimension)
            .panel
    }
}