/*
MIT License

Copyright (c) 2025 Mitja Leino

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

import com.intellij.openapi.components.service
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.JBIntSpinner
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.FormBuilder
import com.mituuz.fuzzier.entities.FuzzyContainer.FilenameType
import com.mituuz.fuzzier.settings.FuzzierGlobalSettingsService
import com.mituuz.fuzzier.settings.FuzzierGlobalSettingsService.RecentFilesMode
import com.mituuz.fuzzier.settings.FuzzierGlobalSettingsService.SearchPosition
import java.awt.Component
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.DefaultListCellRenderer
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JPanel

class FuzzierGlobalSettingsComponent {
    var jPanel: JPanel

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


    /////////////////////////////////////////////////////////////////
    // Popup styling and configuration
    /////////////////////////////////////////////////////////////////
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
            <strong>Note! </strong>This is more performance intensive, you should not use too high file list limit with this option.
    """.trimIndent(),
        false)

    val highlightFilename = SettingsComponent(JBCheckBox(), "Highlight filename in file list",
        """
            Toggles highlighting of the filename on the file list.
            <br>
            Only works with styled file list, which supports html styling.
        """.trimIndent(),
        false)

    val dimensionComponent = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.X_AXIS)
        add(JBLabel("Width: "))
        add(JBIntSpinner(700, 100, 4000))
        add(Box.createHorizontalStrut(10))
        add(JBLabel("Height: "))
        add(JBIntSpinner(400, 100, 4000))
    }
    val defaultDimension = SettingsComponent(dimensionComponent, "Default dimensions",
        """
            Default dimensions for the finder popup. Affects reset window behaviour.<br><br>
            Min: 100, Max: 4000
        """.trimIndent(),
        false)

    val searchPosition = SettingsComponent(ComboBox<SearchPosition>(), "Search bar location",
        """
            Controls where the search bar is located on the popup.
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
            .addComponent(JBLabel("<html><h2>General settings</h2></html>"))
            .addComponent(newTabSelect)
            .addComponent(recentFileModeSelector)
            .addComponent(prioritizeShortDirs)
            .addComponent(debounceTimerValue)
            .addComponent(fileListLimit)

            .addSeparator()
            .addComponent(JBLabel("<html><h2>Popup styling</h2></html>"))
            .addComponent(filenameTypeSelector)
            .addComponent(highlightFilename)
            .addComponent(searchPosition)
            .addComponent(defaultDimension)
            .addComponent(previewFontSize)
            .addComponent(fileListFontSize)
            .addComponent(fileListSpacing)

            .addSeparator()
            .addComponent(JBLabel("<html><h2>Match settings</h2></html>"))
            .addComponent(tolerance)
            .addComponent(multiMatchActive)
            .addComponent(matchWeightSingleChar)
            .addComponent(matchWeightPartialPath)
            .addComponent(matchWeightStreakModifier)
            .addComponent(matchWeightFilename)

            .addSeparator()
            .addComponent(JBLabel("<html><h2>Test bench</h2></html>"))
            .addComponent(startTestBench)
            .addComponent(testBench)
            .addComponentFillVertically(JPanel(), 0)

            .addSeparator()
            .addComponent(JBLabel("<html><h2>Reset window</h2></html>"))
            .addComponent(resetWindowDimension)
            .panel
    }


    private fun setupComponents() {
        multiMatchActive.getCheckBox().addChangeListener {
            matchWeightSingleChar.getIntSpinner().isEnabled = multiMatchActive.getCheckBox().isSelected
        }
        resetWindowDimension.addActionListener {
            service<FuzzierGlobalSettingsService>().state.resetWindow = true
            // Disable the button to indicate that the press was registered
            resetWindowDimension.isEnabled = false
        }

        recentFileModeSelector.getRecentFilesTypeComboBox().renderer = object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(
                list: JList<*>?, value: Any?, index: Int, isSelected: Boolean, cellHasFocus: Boolean
            ): Component {
                val renderer =
                    super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus) as JLabel
                val filesMode = value as RecentFilesMode
                renderer.text = filesMode.text
                return renderer
            }
        }
        for (recentFilesMode in RecentFilesMode.entries) {
            recentFileModeSelector.getRecentFilesTypeComboBox().addItem(recentFilesMode)
        }

        searchPosition.getSearchPositionComboBox().renderer = object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(
                list: JList<*>?,
                value: Any?,
                index: Int,
                isSelected: Boolean,
                cellHasFocus: Boolean
            ): Component? {
                val renderer = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus) as JLabel
                val position = value as SearchPosition
                renderer.text = position.text
                return renderer
            }
        }
        for (sp in SearchPosition.entries) {
            searchPosition.getSearchPositionComboBox().addItem(sp)
        }

        filenameTypeSelector.getFilenameTypeComboBox().renderer = object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(
                list: JList<*>?,
                value: Any?,
                index: Int,
                isSelected: Boolean,
                cellHasFocus: Boolean
            ): Component {
                val renderer =
                    super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus) as JLabel
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
        highlightFilename.getCheckBox().isEnabled =
            filenameTypeSelector.getFilenameTypeComboBox().item == FilenameType.FILENAME_WITH_PATH_STYLED

        startTestBench.getButton().addActionListener {
            startTestBench.getButton().isEnabled = false
            testBench.fill(this)
        }
    }
}