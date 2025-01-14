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
package com.mituuz.fuzzier.settings

import com.intellij.openapi.components.service
import com.intellij.openapi.options.Configurable
import com.mituuz.fuzzier.components.FuzzierSettingsComponent
import com.mituuz.fuzzier.entities.FuzzyContainer.FilenameType
import com.mituuz.fuzzier.settings.FuzzierSettingsService.RecentFilesMode
import javax.swing.JComponent

class FuzzierSettingsConfigurable : Configurable {
    private lateinit var component: FuzzierSettingsComponent
    private var state = service<FuzzierSettingsService>().state
    override fun getDisplayName(): String {
        return "Fuzzy File Finder Settings"
    }

    override fun createComponent(): JComponent {
        component = FuzzierSettingsComponent()

        val combinedString = state.exclusionSet.joinToString("\n")
        component.exclusionSet.getJBTextArea().text = combinedString
        component.ignoredCharacters.getJBTextField().text = state.ignoredCharacters
        component.newTabSelect.getCheckBox().isSelected = state.newTab
        component.recentFileModeSelector.getRecentFilesTypeComboBox().selectedIndex = state.recentFilesMode.ordinal
        component.prioritizeShortDirs.getCheckBox().isSelected = state.prioritizeShorterDirPaths
        component.debounceTimerValue.getIntSpinner().value = state.debouncePeriod
        component.fileListLimit.getIntSpinner().value = state.fileListLimit

        component.filenameTypeSelector.getFilenameTypeComboBox().selectedIndex = state.filenameType.ordinal
        component.highlightFilename.getCheckBox().isSelected = state.highlightFilename
        component.fileListFontSize.getIntSpinner().value = state.fileListFontSize
        component.previewFontSize.getIntSpinner().value = state.previewFontSize
        component.fileListSpacing.getIntSpinner().value = state.fileListSpacing

        component.tolerance.getIntSpinner().value = state.tolerance
        component.multiMatchActive.getCheckBox().isSelected = state.multiMatch
        component.matchWeightPartialPath.getIntSpinner().value = state.matchWeightPartialPath
        component.matchWeightSingleChar.getIntSpinner().value = state.matchWeightSingleChar
        component.matchWeightSingleChar.getIntSpinner().isEnabled = state.multiMatch
        component.matchWeightStreakModifier.getIntSpinner().value = state.matchWeightStreakModifier
        component.matchWeightFilename.getIntSpinner().value = state.matchWeightFilename
        return component.jPanel
    }

    override fun isModified(): Boolean {
        val newSet = component.exclusionSet.getJBTextArea().text
            .split("\n")
            .filter { it.isNotBlank() }
            .toSet()

        return state.exclusionSet != newSet
                || state.ignoredCharacters != component.ignoredCharacters.getJBTextField().text
                || state.newTab != component.newTabSelect.getCheckBox().isSelected
                || state.recentFilesMode != component.recentFileModeSelector.getRecentFilesTypeComboBox().selectedItem
                || state.prioritizeShorterDirPaths != component.prioritizeShortDirs.getCheckBox().isSelected
                || state.debouncePeriod != component.debounceTimerValue.getIntSpinner().value
                || state.fileListLimit != component.fileListLimit.getIntSpinner().value

                || state.filenameType != component.filenameTypeSelector.getFilenameTypeComboBox().selectedItem
                || state.highlightFilename != component.highlightFilename.getCheckBox().isSelected
                || state.fileListFontSize != component.fileListFontSize.getIntSpinner().value
                || state.previewFontSize != component.previewFontSize.getIntSpinner().value
                || state.fileListSpacing != component.fileListSpacing.getIntSpinner().value

                || state.tolerance != component.tolerance.getIntSpinner().value
                || state.multiMatch != component.multiMatchActive.getCheckBox().isSelected
                || state.matchWeightPartialPath != component.matchWeightPartialPath.getIntSpinner().value
                || state.matchWeightSingleChar != component.matchWeightSingleChar.getIntSpinner().value
                || state.matchWeightStreakModifier != component.matchWeightStreakModifier.getIntSpinner().value
                || state.matchWeightFilename != component.matchWeightFilename.getIntSpinner().value
    }

    override fun apply() {
        val newSet = component.exclusionSet.getJBTextArea().text
            .split("\n")
            .filter { it.isNotBlank() }
            .toSet()
        state.exclusionSet = newSet as MutableSet<String>
        state.ignoredCharacters = component.ignoredCharacters.getJBTextField().text
        state.newTab = component.newTabSelect.getCheckBox().isSelected
        state.recentFilesMode = RecentFilesMode.entries.toTypedArray()[component.recentFileModeSelector.getRecentFilesTypeComboBox().selectedIndex]
        state.prioritizeShorterDirPaths = component.prioritizeShortDirs.getCheckBox().isSelected
        state.debouncePeriod = component.debounceTimerValue.getIntSpinner().value as Int
        state.fileListLimit = component.fileListLimit.getIntSpinner().value as Int

        state.filenameType = FilenameType.entries.toTypedArray()[component.filenameTypeSelector.getFilenameTypeComboBox().selectedIndex]
        state.highlightFilename = component.highlightFilename.getCheckBox().isSelected
        state.fileListFontSize = component.fileListFontSize.getIntSpinner().value as Int
        state.previewFontSize = component.previewFontSize.getIntSpinner().value as Int
        state.fileListSpacing = component.fileListSpacing.getIntSpinner().value as Int

        state.tolerance = component.tolerance.getIntSpinner().value as Int
        state.multiMatch = component.multiMatchActive.getCheckBox().isSelected
        state.matchWeightPartialPath = component.matchWeightPartialPath.getIntSpinner().value as Int
        state.matchWeightSingleChar = component.matchWeightSingleChar.getIntSpinner().value as Int
        state.matchWeightStreakModifier = component.matchWeightStreakModifier.getIntSpinner().value as Int
        state.matchWeightFilename = component.matchWeightFilename.getIntSpinner().value as Int
    }
}