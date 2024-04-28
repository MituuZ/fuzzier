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
import com.mituuz.fuzzier.components.FuzzierSettingsComponent.SettingsComponent
import com.mituuz.fuzzier.entities.FuzzyMatchContainer.FilenameType
import com.mituuz.fuzzier.entities.FuzzyMatchContainer.FilenameType.*
import com.mituuz.fuzzier.settings.FuzzierSettingsService
import java.awt.Component
import javax.swing.*
import javax.swing.border.LineBorder

class FuzzierSettingsComponent {
    var jPanel: JPanel

    /////////////////////////////////////////////////////////////////
    // General settings
    /////////////////////////////////////////////////////////////////
    val exclusionSet = SettingsComponent(JBTextArea(), "File path exclusions",
        """
            One line per one exclusion from the Fuzzier results.<br><br>
            Empty lines are skipped and all files in the project root start with "/"<br><br>
            Supports wildcards (*) for starts with and ends with. Defaults to contains if no wildcards are present.<br><br>
            e.g. "kt" excludes all files/file paths that contain the "kt" string. (main.<strong>kt</strong>, <strong>kt</strong>lin.java)
    """.trimIndent())

    val newTabSelect = SettingsComponent(JBCheckBox(), "Open files in a new tab")

    val prioritizeShortDirs = SettingsComponent(JBCheckBox(), "Prioritize shorter dir paths", """
        When having a directory selector active, prioritize shorter file paths over pure score calculation.
    """.trimIndent(),
        false)

    val debounceTimerValue = SettingsComponent(JBIntSpinner(150, 0, 2000), "Debounce period (ms)",
        """
            Controls how long the search field must be idle before starting the search process.
        """.trimIndent(),
        false)

    val filenameTypeSelector = SettingsComponent(ComboBox<FilenameType>(), "Filename type",
        """
            Controls how the filename is shown on the file search and selector popups.<br><br>
            Choices are as follows (/path/to/file):<br><br>
            
            <strong>Full path</strong> - Shows the full path of the file:
            <br>
            /path/to/file
            
            <br><br>
            <strong>Filename only</strong> - Shows only the filename:
            <br>
            file
            
            <br><br>
            <strong>Filename with (path)</strong> - Shows path in brackets:
            <br>
            file (/path/to/file)
            
            <br><br>
            <strong>Filename with (path) styled</strong> - Uses bold for filename and italic for the path:
            <br>
            <strong>file</strong>  <i>(path/to/file)</i>
            <br>
            <strong>Note!</strong>This is more performance intensive, you should not use too high file list limit with this option.
    """.trimIndent(),
        false)

    val fileListLimit = SettingsComponent(JBIntSpinner(50, 1, 5000), "File list limit",
        """
            Controls how many files are shown and listed on the popup.
        """.trimIndent(),
        false)

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


    /////////////////////////////////////////////////////////////////
    // Match settings
    /////////////////////////////////////////////////////////////////
    val tolerance = SettingsComponent(JBIntSpinner(0, 0, 5), "Match tolerance",
        """
            How many non-matching letters are allowed when calculating matches.<br><br>
            e.g. korlin would still match kotlin.
        """.trimIndent(),
        false)

    val multiMatchActive = SettingsComponent(JBCheckBox(), "Match characters multiple times",
        """
            Count score for each instance of a character in the search string.<br><br>
            Normally file list sorting is done based on the longest streak,
            but similar package or folder names might make finding correct files inconvenient.<br><br>
            e.g. kotlin/is/fun contains "i" two times, so search "if" would score three points.
        """.trimIndent(),
        false)

    val matchWeightPartialPath = SettingsComponent(JBIntSpinner(10, 0, 100), "Match weight: Partial path match",
        """
            How much score should a partial path match give.<br><br>
            Partial matches are checked against file path parts, delimited by "/" and ".".<br><br>
            e.g. search string "is" is a partial path match for kotlin/<strong>is</strong>/fun, where as "isf" is not.
        """.trimIndent(),
        false)

    val matchWeightSingleChar = SettingsComponent(JBIntSpinner(5, 0, 50), "Match weight: Single char (* 0.1)",
        """
            How much score should a single char give. Only applies when multi match is active.<br><br>
            Is divided by 10 when calculating score.
        """.trimIndent(),
        false)

    val matchWeightStreakModifier = SettingsComponent(JBIntSpinner(10, 0, 100), "Match weight: Streak modifier (* 0.1)",
        """
            Longest streak score is multiplied by this amount (divided by 10).<br><br>
            e.g. 10 = 1, so highest streak is added as the number of matched letters.
        """.trimIndent(),
        false)

    val matchWeightFilename = SettingsComponent(JBIntSpinner(5, 0, 100), "Match weight: Filename modifier (* 0.1)",
        """
            How much score should a filename match give (divided by 10). Considers the longest streak that matches.<br><br>
            e.g. search string "file" is a filename match for kotlin/<strong>file</strong>.kt
        """.trimIndent(),
        false)

    private val startTestBench = SettingsComponent(JButton("Launch Test Bench"), "Test Bench",
        """
            Test settings live with the current project's file index.
        """.trimIndent())
    private var testBench = TestBenchComponent()

    private var resetWindowDimension = JButton("Reset popup location")

    init {
        setupComponents()
        jPanel = FormBuilder.createFormBuilder()
            .addComponent(exclusionSet)
            .addSeparator()
            .addComponent(newTabSelect)
            .addComponent(prioritizeShortDirs)
            .addComponent(debounceTimerValue)
            .addComponent(filenameTypeSelector)
            .addComponent(fileListLimit)
            .addComponent(fontSize)
            .addComponent(fileListSpacing)

            .addSeparator()
            .addComponent(JBLabel("<html><strong>Match settings</strong></html>"))
            .addComponent(tolerance)
            .addComponent(multiMatchActive)
            .addComponent(matchWeightSingleChar)
            .addComponent(matchWeightPartialPath)
            .addComponent(matchWeightStreakModifier)
            .addComponent(matchWeightFilename)

            .addComponent(startTestBench)
            .addComponent(testBench)
            .addComponentFillVertically(JPanel(), 0)
            .addComponent(resetWindowDimension)
            .panel
    }

    private fun setupComponents() {
        multiMatchActive.getCheckBox().addChangeListener {
            matchWeightSingleChar.getIntSpinner().isEnabled = multiMatchActive.getCheckBox().isSelected
        }
        exclusionSet.component.border = LineBorder(JBColor.BLACK, 1)
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

        startTestBench.getButton().addActionListener {
            startTestBench.getButton().isEnabled = false
            testBench.fill(this)
        }
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

        fun getCheckBox(): JBCheckBox {
            return component as JBCheckBox
        }

        fun getIntSpinner(): JBIntSpinner {
            return component as JBIntSpinner
        }

        fun getFilenameTypeComboBox(): ComboBox<FilenameType> {
            @Suppress("UNCHECKED_CAST")
            return component as ComboBox<FilenameType>
        }

        fun getButton(): JButton {
            return component as JButton
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