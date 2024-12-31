/*
MIT License

Copyright (c) 2024 Mitja Leino

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/
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
import com.intellij.ui.components.JBTextField
import com.mituuz.fuzzier.components.FuzzierSettingsComponent.SettingsComponent
import com.mituuz.fuzzier.entities.FuzzyMatchContainer.FilenameType
import com.mituuz.fuzzier.settings.FuzzierSettingsService
import com.mituuz.fuzzier.settings.FuzzierSettingsService.RecentFilesMode
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
            Exclusions apply to Fuzzier search and FuzzyMover. One line per one exclusion.<br><br>
            Empty lines are skipped and all files in the project root start with "/"<br><br>
            Supports wildcards (*) for starts with and ends with. Defaults to contains if no wildcards are present.<br><br>
            e.g. "kt" excludes all files/file paths that contain the "kt" string. (main.<strong>kt</strong>, <strong>kt</strong>lin.java)
    """.trimIndent())

    val ignoredCharacters = SettingsComponent(JBTextField(), "Ignored characters",
        """
            Exclude characters from affecting the search. Any character added here will be skipped during the search.<br>
            This could be useful for example when copy pasting similar file paths.<br><br>
            e.g. "%" would transform a search string like "%%%kot%%lin" to "kotlin"
        """.trimIndent(),
        false)

    val newTabSelect = SettingsComponent(JBCheckBox(), "Open files in a new tab")

    val recentFileModeSelector = SettingsComponent(ComboBox<RecentFilesMode>(), "Show recent files on start", """
        Show recent files when opening a search window.
    """.trimIndent(),
        false)

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

    val highlightFilename = SettingsComponent(JBCheckBox(), "Highlight filename in file list",
        """
            Toggles highlighting of the filename on the file list.
            <br>
            Only works with styled file list, which supports html styling.
        """.trimIndent(),
        false)

    val fileListLimit = SettingsComponent(JBIntSpinner(50, 1, 5000), "File list limit",
        """
            Controls how many files are shown and listed on the popup.
        """.trimIndent(),
        false)

    val fileListFontSize = SettingsComponent(JBIntSpinner(14, 4, 30), "File list font size",
        """
            Controls the font size of the file list in the search and selector popups.
        """.trimIndent(),
        false)

    val previewFontSize = SettingsComponent(JBIntSpinner(0, 0, 30), "Preview font size",
        """
            Controls the font size of the preview in the search and selector popups.
            <br>
            When value is zero, use the current font size of the editor.
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
            .addComponent(JBLabel("<html><strong>General settings</strong></html>"))
            .addComponent(exclusionSet)
            .addComponent(ignoredCharacters)

            .addSeparator()
            .addComponent(newTabSelect)
            .addComponent(recentFileModeSelector)
            .addComponent(prioritizeShortDirs)
            .addComponent(debounceTimerValue)
            .addComponent(fileListLimit)

            .addSeparator()
            .addComponent(JBLabel("<html><strong>Popup styling</strong></html>"))
            .addComponent(filenameTypeSelector)
            .addComponent(highlightFilename)
            .addComponent(fileListFontSize)
            .addComponent(previewFontSize)
            .addComponent(fileListSpacing)

            .addSeparator()
            .addComponent(JBLabel("<html><strong>Match settings</strong></html>"))
            .addComponent(tolerance)
            .addComponent(multiMatchActive)
            .addComponent(matchWeightSingleChar)
            .addComponent(matchWeightPartialPath)
            .addComponent(matchWeightStreakModifier)
            .addComponent(matchWeightFilename)

            .addSeparator()
            .addComponent(startTestBench)
            .addComponent(testBench)
            .addComponentFillVertically(JPanel(), 0)

            .addSeparator()
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

        recentFileModeSelector.getRecentFilesTypeComboBox().renderer = object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(
                list: JList<*>?, value: Any?, index: Int, isSelected: Boolean, cellHasFocus: Boolean
            ): Component {
                val renderer = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus) as JLabel
                val filesMode = value as RecentFilesMode
                renderer.text = filesMode.text
                return renderer
            }
        }
        for (recentFilesMode in RecentFilesMode.entries) {
            recentFileModeSelector.getRecentFilesTypeComboBox().addItem(recentFilesMode)
        }

        filenameTypeSelector.getFilenameTypeComboBox().renderer = object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(list: JList<*>?, value: Any?, index: Int, isSelected: Boolean, cellHasFocus: Boolean): Component {
                val renderer = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus) as JLabel
                val filenameType = value as FilenameType
                renderer.text = filenameType.text
                return renderer
            }
        }
        for (filenameType in FilenameType.entries) {
            filenameTypeSelector.getFilenameTypeComboBox().addItem(filenameType)
        }
        filenameTypeSelector.getFilenameTypeComboBox().addItemListener {
            highlightFilename.getCheckBox().isEnabled = it.item == FilenameType.FILENAME_WITH_PATH_STYLED
        }
        highlightFilename.getCheckBox().isEnabled = filenameTypeSelector.getFilenameTypeComboBox().item == FilenameType.FILENAME_WITH_PATH_STYLED

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

        fun getJBTextField(): JBTextField {
            return component as JBTextField
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

        fun getRecentFilesTypeComboBox(): ComboBox<RecentFilesMode> {
            @Suppress("UNCHECKED_CAST")
            return component as ComboBox<RecentFilesMode>
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