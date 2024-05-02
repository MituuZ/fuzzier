package com.mituuz.fuzzier.settings

import com.intellij.openapi.components.service
import com.intellij.openapi.options.Configurable
import com.mituuz.fuzzier.components.FuzzierSettingsComponent
import com.mituuz.fuzzier.entities.FuzzyMatchContainer.FilenameType
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
        component.newTabSelect.getCheckBox().isSelected = state.newTab
        component.prioritizeShortDirs.getCheckBox().isSelected = state.prioritizeShorterDirPaths
        component.debounceTimerValue.getIntSpinner().value = state.debouncePeriod
        component.filenameTypeSelector.getFilenameTypeComboBox().selectedIndex = state.filenameType.ordinal
        component.fileListLimit.getIntSpinner().value = state.fileListLimit
        component.fontSize.getIntSpinner().value = state.fontSize
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
                || state.newTab != component.newTabSelect.getCheckBox().isSelected
                || state.prioritizeShorterDirPaths != component.prioritizeShortDirs.getCheckBox().isSelected
                || state.debouncePeriod != component.debounceTimerValue.getIntSpinner().value
                || state.filenameType != component.filenameTypeSelector.getFilenameTypeComboBox().selectedItem
                || state.fileListLimit != component.fileListLimit.getIntSpinner().value
                || state.fontSize != component.fontSize.getIntSpinner().value
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
        state.newTab = component.newTabSelect.getCheckBox().isSelected
        state.prioritizeShorterDirPaths = component.prioritizeShortDirs.getCheckBox().isSelected
        state.debouncePeriod = component.debounceTimerValue.getIntSpinner().value as Int
        state.filenameType = FilenameType.entries.toTypedArray()[component.filenameTypeSelector.getFilenameTypeComboBox().selectedIndex]
        state.fileListLimit = component.fileListLimit.getIntSpinner().value as Int
        state.fontSize = component.fontSize.getIntSpinner().value as Int
        state.fileListSpacing = component.fileListSpacing.getIntSpinner().value as Int

        state.tolerance = component.tolerance.getIntSpinner().value as Int
        state.multiMatch = component.multiMatchActive.getCheckBox().isSelected
        state.matchWeightPartialPath = component.matchWeightPartialPath.getIntSpinner().value as Int
        state.matchWeightSingleChar = component.matchWeightSingleChar.getIntSpinner().value as Int
        state.matchWeightStreakModifier = component.matchWeightStreakModifier.getIntSpinner().value as Int
        state.matchWeightFilename = component.matchWeightFilename.getIntSpinner().value as Int
    }
}