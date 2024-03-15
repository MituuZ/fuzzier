package com.mituuz.fuzzier.components

import com.intellij.icons.AllIcons
import com.intellij.openapi.components.service
import com.intellij.ui.JBColor
import com.intellij.ui.JBIntSpinner
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.FormBuilder
import com.intellij.openapi.ui.ComboBox
import com.mituuz.fuzzier.StringEvaluator.FilenameType
import com.mituuz.fuzzier.StringEvaluator.FilenameType.*
import com.mituuz.fuzzier.settings.FuzzierSettingsService
import java.awt.Component
import javax.swing.*
import javax.swing.border.LineBorder

class FuzzierSettingsComponent {
    var jPanel: JPanel
    var exclusionList = JBTextArea()
    private var exclusionInstructions = JBLabel("<html><strong>File path exclusions:</strong><br>", AllIcons.General.ContextHelp, JBLabel.LEFT)
    var newTabSelect = JBCheckBox()
    var debounceTimerValue = JBIntSpinner(150, 0, 2000)
    private var debounceInstructions = JBLabel("<html><strong>Debounce period (ms)</strong></html>", AllIcons.General.ContextHelp, JBLabel.LEFT)
    private var resetWindowDimension = JButton("Reset popup location")
    private var filenameTypeInstructions = JBLabel("<html><strong>Filename type</strong></html>", AllIcons.General.ContextHelp, JBLabel.LEFT)
    var filenameTypeSelector = ComboBox<FilenameType>()

    var multiMatchActive = JBCheckBox()
    private var multiMatchInstructions = JBLabel("<html><strong>Match characters multiple times</strong></html>", AllIcons.General.ContextHelp, JBLabel.LEFT)
    var matchWeightPartialPath = JBIntSpinner(10, 0, 100)
    private var partialPathInfo = JBLabel("<html><strong>Match weight: Partial path match</strong></html>", AllIcons.General.ContextHelp, JBLabel.LEFT)
    var matchWeightSingleChar = JBIntSpinner(5, 0, 50)
    private var singleCharInfo = JBLabel("<html><strong>Match weight: Single char (* 0.1)</strong></html>", AllIcons.General.ContextHelp, JBLabel.LEFT)
    var matchWeightStreakModifier = JBIntSpinner(10, 0, 100)
    private var streakModifierInfo = JBLabel("<html><strong>Match weight: Streak modifier (* 0.1)</strong></html>", AllIcons.General.ContextHelp, JBLabel.LEFT)

    private var startTestBench = JButton("Launch Test Bench", AllIcons.General.ContextHelp)
    private var testBench = TestBenchComponent()

    init {
        setupComponents()
        jPanel = FormBuilder.createFormBuilder()
            .addComponent(exclusionInstructions)
            .addComponent(exclusionList)
            .addSeparator()
            .addLabeledComponent("<html><strong>Open files in a new tab</strong></html>", newTabSelect)
            .addLabeledComponent(debounceInstructions, debounceTimerValue)
            .addLabeledComponent(filenameTypeInstructions, filenameTypeSelector)

            .addSeparator()
            .addComponent(JBLabel("<html><strong>Match settings</strong></html>"))
            .addLabeledComponent(multiMatchInstructions, multiMatchActive)
            .addLabeledComponent(singleCharInfo, matchWeightSingleChar)
            .addLabeledComponent(partialPathInfo, matchWeightPartialPath)
            .addLabeledComponent(streakModifierInfo, matchWeightStreakModifier)
            .addComponent(startTestBench)
            .addComponent(testBench)
            .addComponentFillVertically(JPanel(), 0)
            .addComponent(resetWindowDimension)
            .panel
    }

    private fun setupComponents() {
        multiMatchActive.addChangeListener {
            matchWeightSingleChar.isEnabled = multiMatchActive.isSelected
        }
        exclusionList.border = LineBorder(JBColor.BLACK, 1)
        resetWindowDimension.addActionListener {
            service<FuzzierSettingsService>().state.resetWindow = true
        }

        filenameTypeSelector.renderer = object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(list: JList<*>?, value: Any?, index: Int, isSelected: Boolean, cellHasFocus: Boolean): Component {
                val renderer = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus) as JLabel
                val filenameType = value as FilenameType
                renderer.text = filenameType.text
                return renderer
            }
        }
        for (filenameType in entries) {
            filenameTypeSelector.addItem(filenameType)
        }

        startTestBench.addActionListener {
            startTestBench.isEnabled = false
            testBench.fill(this)
        }

        exclusionInstructions.toolTipText = """
            One line per one exclusion from the Fuzzier results.<br><br>
            Empty lines are skipped and all files in the project root start with "/"<br><br>
            Supports wildcards (*) for starts with and ends with. Defaults to contains if no wildcards are present.<br><br>
            e.g. "kt" excludes all files/file paths that contain the "kt" string. (main.<strong>kt</strong>, <strong>kt</strong>lin.java)
        """.trimIndent()

        multiMatchInstructions.toolTipText = """
            Count score for each instance of a character in the search string.<br><br>
            Normally file list sorting is done based on the longest streak,
            but similar package or folder names might make finding correct files inconvenient.<br><br>
            e.g. kotlin/is/fun contains "i" two times, so search "if" would score three points.
            
        """.trimIndent()
        debounceInstructions.toolTipText = """
            Controls how long the search field must be idle before starting the search process.
        """.trimIndent()

        partialPathInfo.toolTipText = """
            How much score should a partial path match give.<br><br>
            Partial matches are checked against file path parts, delimited by "/" and ".".<br><br>
            e.g. search string "is" is a partial path match for kotlin/<strong>is</strong>/fun, where as "isf" is not.
        """.trimIndent()

        singleCharInfo.toolTipText = """
            How much score should a single char give. Only applies when multi match is active.<br><br>
            Is divided by 10 when calculating score.
        """.trimIndent()

        streakModifierInfo.toolTipText = """
            Longest streak score is multiplied by this amount (divided by 10).<br><br>
            e.g. 10 = 1, so highest streak is added as the number of matched letters.
        """.trimIndent()

        startTestBench.toolTipText = """
            Test settings changes live on the current project's file index.
        """.trimIndent()

        filenameTypeInstructions.toolTipText = """
            Controls how the filename is shown on the file search and selector popups.
        """.trimIndent()
    }
}