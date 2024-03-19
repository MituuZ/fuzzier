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
import com.mituuz.fuzzier.components.FuzzierSettingsComponent.SettingsComponent
import com.mituuz.fuzzier.settings.FuzzierSettingsService
import java.awt.Component
import javax.swing.*
import javax.swing.border.LineBorder

class FuzzierSettingsComponent {
    var jPanel: JPanel

    val exclusionList = SettingsComponent(JBTextArea(), "File path exclusions",
        """
            One line per one exclusion from the Fuzzier results.<br><br>
            Empty lines are skipped and all files in the project root start with "/"<br><br>
            Supports wildcards (*) for starts with and ends with. Defaults to contains if no wildcards are present.<br><br>
            e.g. "kt" excludes all files/file paths that contain the "kt" string. (main.<strong>kt</strong>, <strong>kt</strong>lin.java)
    """.trimIndent())

    val newTabSelect = SettingsComponent(JBCheckBox(), "Open files in a new tab")

    val debounceTimerValue = SettingsComponent(JBIntSpinner(150, 0, 2000), "Debounce period (ms)",
        """
            Controls how long the search field must be idle before starting the search process.
        """.trimIndent(),
        false)

    val filenameTypeSelector = SettingsComponent(ComboBox<FilenameType>(), "Filename type",
        """
            Controls how the filename is shown on the file search and selector popups.<br><br>
            Choices are as follows (/path/to/file):<br><br>
            <strong>Full path</strong> - Shows the full path of the file: /path/to/file<br>
            <strong>Filename only</strong> - Shows only the filename: file<br>
            <strong>Filename with (path)</strong> - Shows path in brackets: file (/path/to/file)
    """.trimIndent(),
        false)

    val boldFilenameWithType = SettingsComponent(JBCheckBox(), "Bold filename when using: Filename with (path)")

    val fontSize = SettingsComponent(JBIntSpinner(14, 4, 20), "File list font size",
        """
            Controls the font size of the file list in the search and selector popups.
        """.trimIndent(),
        false)

    val fileListSpacing = SettingsComponent(JBIntSpinner(0, 0, 10), "File list vertical spacing",
        """
            Controls the vertical spacing between the file list items in the search and selector popups.
        """.trimIndent(),
        false)

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

    private var resetWindowDimension = JButton("Reset popup location")

    init {
        setupComponents()
        jPanel = FormBuilder.createFormBuilder()
            .addComponent(exclusionList)
            .addSeparator()
            .addComponent(newTabSelect)
            .addComponent(debounceTimerValue)
            .addComponent(filenameTypeSelector)
            .addComponent(boldFilenameWithType)
            .addComponent(fontSize)
            .addComponent(fileListSpacing)

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
        filenameTypeSelector.getFilenameTypeComboBox().addActionListener {
            boldFilenameWithType.getJBCheckBox().isEnabled = filenameTypeSelector.getFilenameTypeComboBox().selectedItem == FILENAME_WITH_PATH
        }
        exclusionList.component.border = LineBorder(JBColor.BLACK, 1)
        resetWindowDimension.addActionListener {
            service<FuzzierSettingsService>().state.resetWindow = true
        }

        filenameTypeSelector.getFilenameTypeComboBox().renderer = object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(list: JList<*>?, value: Any?, index: Int, isSelected: Boolean, cellHasFocus: Boolean): Component {
                val renderer = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus) as JLabel
                val filenameType = value as FilenameType
                renderer.text = filenameType.text
                return renderer
            }
        }
        for (filenameType in entries) {
            filenameTypeSelector.getFilenameTypeComboBox().addItem(filenameType)
        }

        startTestBench.addActionListener {
            startTestBench.isEnabled = false
            testBench.fill(this)
        }

        multiMatchInstructions.toolTipText = """
            Count score for each instance of a character in the search string.<br><br>
            Normally file list sorting is done based on the longest streak,
            but similar package or folder names might make finding correct files inconvenient.<br><br>
            e.g. kotlin/is/fun contains "i" two times, so search "if" would score three points.
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
    }

    class SettingsComponent {
        val includesSeparateLabel: Boolean
        val onlyTitle: Boolean
        val component: JComponent
        val title: String
        private val description: String
        var label: JBLabel

        constructor(component: JComponent, title: String, description: String, separateLabel: Boolean = true) {
            this.onlyTitle = false
            this.includesSeparateLabel = separateLabel
            this.component = component
            this.title = title
            this.description = description

            val strongTitle = "<html><strong>$title</strong></html>"
            label = JBLabel(strongTitle, AllIcons.General.ContextHelp, JBLabel.LEFT)
            label.toolTipText = description
        }

        constructor(component: JComponent, title: String) {
            this.onlyTitle = true
            this.includesSeparateLabel = false
            this.component = component
            this.title = title
            this.description = ""
            this.label = JBLabel()
        }

        fun getJBTextArea(): JBTextArea {
            return component as JBTextArea
        }

        fun getJBCheckBox(): JBCheckBox {
            return component as JBCheckBox
        }

        fun getIntSpinner(): JBIntSpinner {
            return component as JBIntSpinner
        }

        fun getFilenameTypeComboBox(): ComboBox<FilenameType> {
            @Suppress("UNCHECKED_CAST")
            return component as ComboBox<FilenameType>
        }
    }
}

private fun FormBuilder.addLabeledComponent(settingsComponent: SettingsComponent): FormBuilder {
    return addLabeledComponent(settingsComponent.title, settingsComponent.component)
}

private fun FormBuilder.addComponent(settingsComponent: SettingsComponent): FormBuilder {
    if (!settingsComponent.includesSeparateLabel && !settingsComponent.onlyTitle) {
        return addLabeledComponent(settingsComponent.label, settingsComponent.component)
    }

    if (settingsComponent.onlyTitle) {
        return addLabeledComponent(settingsComponent)
    }

    val builder = addComponent(settingsComponent.label)
    return builder.addComponent(settingsComponent.component)
}