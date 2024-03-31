package com.mituuz.fuzzier.settings

import com.intellij.openapi.components.service
import com.intellij.openapi.options.Configurable
import com.mituuz.fuzzier.components.FuzzierSettingsComponent
import com.mituuz.fuzzier.entities.FuzzyMatchContainer.FilenameType
import javax.swing.JComponent

class FuzzierSettingsConfigurable : Configurable {
    private lateinit var fuzzierSettingsComponent: FuzzierSettingsComponent
    private var fuzzierSettingsService = service<FuzzierSettingsService>()
    override fun getDisplayName(): String {
        return "Fuzzy File Finder Settings"
    }

    override fun createComponent(): JComponent {
        fuzzierSettingsComponent = FuzzierSettingsComponent()

        val combinedString = fuzzierSettingsService.state.exclusionSet.joinToString("\n")
        fuzzierSettingsComponent.exclusionSet.getJBTextArea().text = combinedString
        fuzzierSettingsComponent.newTabSelect.getCheckBox().isSelected = fuzzierSettingsService.state.newTab
        fuzzierSettingsComponent.debounceTimerValue.getIntSpinner().value = fuzzierSettingsService.state.debouncePeriod
        fuzzierSettingsComponent.filenameTypeSelector.getFilenameTypeComboBox().selectedIndex = fuzzierSettingsService.state.filenameType.ordinal
        fuzzierSettingsComponent.fontSize.getIntSpinner().value = fuzzierSettingsService.state.fontSize
        fuzzierSettingsComponent.fileListSpacing.getIntSpinner().value = fuzzierSettingsService.state.fileListSpacing

        fuzzierSettingsComponent.multiMatchActive.getCheckBox().isSelected = fuzzierSettingsService.state.multiMatch
        fuzzierSettingsComponent.matchWeightPartialPath.getIntSpinner().value = fuzzierSettingsService.state.matchWeightPartialPath
        fuzzierSettingsComponent.matchWeightSingleChar.getIntSpinner().value = fuzzierSettingsService.state.matchWeightSingleChar
        fuzzierSettingsComponent.matchWeightSingleChar.getIntSpinner().isEnabled = fuzzierSettingsService.state.multiMatch
        fuzzierSettingsComponent.matchWeightStreakModifier.getIntSpinner().value = fuzzierSettingsService.state.matchWeightStreakModifier
        return fuzzierSettingsComponent.jPanel
    }

    override fun isModified(): Boolean {
        return fuzzierSettingsService.state.exclusionSet != fuzzierSettingsComponent.exclusionSet.getJBTextArea().text.split("\n")
                || fuzzierSettingsService.state.newTab != fuzzierSettingsComponent.newTabSelect.getCheckBox().isSelected
                || fuzzierSettingsService.state.debouncePeriod != fuzzierSettingsComponent.debounceTimerValue.getIntSpinner().value
                || fuzzierSettingsService.state.filenameType != fuzzierSettingsComponent.filenameTypeSelector.getFilenameTypeComboBox().selectedItem
                || fuzzierSettingsService.state.fontSize != fuzzierSettingsComponent.fontSize.getIntSpinner().value
                || fuzzierSettingsService.state.fileListSpacing != fuzzierSettingsComponent.fileListSpacing.getIntSpinner().value

                || fuzzierSettingsService.state.multiMatch != fuzzierSettingsComponent.multiMatchActive.getCheckBox().isSelected
                || fuzzierSettingsService.state.matchWeightPartialPath != fuzzierSettingsComponent.matchWeightPartialPath.getIntSpinner().value
                || fuzzierSettingsService.state.matchWeightSingleChar != fuzzierSettingsComponent.matchWeightSingleChar.getIntSpinner().value
                || fuzzierSettingsService.state.matchWeightStreakModifier != fuzzierSettingsComponent.matchWeightStreakModifier.getIntSpinner().value
    }

    override fun apply() {
        val newSet = fuzzierSettingsComponent.exclusionSet.getJBTextArea().text
            .split("\n")
            .filter { it.isNotBlank() }
            .toSet()
        fuzzierSettingsService.state.exclusionSet = newSet as MutableSet<String>
        fuzzierSettingsService.state.newTab = fuzzierSettingsComponent.newTabSelect.getCheckBox().isSelected
        fuzzierSettingsService.state.debouncePeriod = fuzzierSettingsComponent.debounceTimerValue.getIntSpinner().value as Int
        fuzzierSettingsService.state.filenameType = FilenameType.entries.toTypedArray()[fuzzierSettingsComponent.filenameTypeSelector.getFilenameTypeComboBox().selectedIndex]
        fuzzierSettingsService.state.fontSize = fuzzierSettingsComponent.fontSize.getIntSpinner().value as Int
        fuzzierSettingsService.state.fileListSpacing = fuzzierSettingsComponent.fileListSpacing.getIntSpinner().value as Int
        
        fuzzierSettingsService.state.multiMatch = fuzzierSettingsComponent.multiMatchActive.getCheckBox().isSelected
        fuzzierSettingsService.state.matchWeightPartialPath = fuzzierSettingsComponent.matchWeightPartialPath.getIntSpinner().value as Int
        fuzzierSettingsService.state.matchWeightSingleChar = fuzzierSettingsComponent.matchWeightSingleChar.getIntSpinner().value as Int
        fuzzierSettingsService.state.matchWeightStreakModifier = fuzzierSettingsComponent.matchWeightStreakModifier.getIntSpinner().value as Int
    }
}