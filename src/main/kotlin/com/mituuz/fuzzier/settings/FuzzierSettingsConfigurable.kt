package com.mituuz.fuzzier.settings

import com.intellij.openapi.components.service
import com.intellij.openapi.options.Configurable
import com.mituuz.fuzzier.StringEvaluator.FilenameType
import com.mituuz.fuzzier.components.FuzzierSettingsComponent
import javax.swing.JComponent

class FuzzierSettingsConfigurable : Configurable {
    private lateinit var fuzzierSettingsComponent: FuzzierSettingsComponent
    private var fuzzierSettingsService = service<FuzzierSettingsService>()
    override fun getDisplayName(): String {
        return "Fuzzy File Finder Settings"
    }

    override fun createComponent(): JComponent {
        fuzzierSettingsComponent = FuzzierSettingsComponent()

        val list = fuzzierSettingsService.state.exclusionList
        val combinedString = list.indices.joinToString("\n") { index ->
            list[index]
        }
        fuzzierSettingsComponent.exclusionList.getJBTextArea().text = combinedString
        fuzzierSettingsComponent.newTabSelect.getJBCheckBox().isSelected = fuzzierSettingsService.state.newTab
        fuzzierSettingsComponent.debounceTimerValue.getIntSpinner().value = fuzzierSettingsService.state.debouncePeriod
        fuzzierSettingsComponent.filenameTypeSelector.getFilenameTypeComboBox().selectedIndex = fuzzierSettingsService.state.filenameType.ordinal
        fuzzierSettingsComponent.boldFilenameWithType.getJBCheckBox().isSelected = fuzzierSettingsService.state.boldFilenameWithType
        fuzzierSettingsComponent.fontSize.getIntSpinner().value = fuzzierSettingsService.state.fontSize
        fuzzierSettingsComponent.fileListSpacing.getIntSpinner().value = fuzzierSettingsService.state.fileListSpacing

        fuzzierSettingsComponent.multiMatchActive.isSelected = fuzzierSettingsService.state.multiMatch
        fuzzierSettingsComponent.matchWeightPartialPath.value = fuzzierSettingsService.state.matchWeightPartialPath
        fuzzierSettingsComponent.matchWeightSingleChar.value = fuzzierSettingsService.state.matchWeightSingleChar
        fuzzierSettingsComponent.matchWeightSingleChar.isEnabled = fuzzierSettingsService.state.multiMatch
        fuzzierSettingsComponent.matchWeightStreakModifier.value = fuzzierSettingsService.state.matchWeightStreakModifier
        return fuzzierSettingsComponent.jPanel
    }

    override fun isModified(): Boolean {
        return fuzzierSettingsService.state.exclusionList != fuzzierSettingsComponent.exclusionList.getJBTextArea().text.split("\n")
                || fuzzierSettingsService.state.newTab != fuzzierSettingsComponent.newTabSelect.getJBCheckBox().isSelected
                || fuzzierSettingsService.state.debouncePeriod != fuzzierSettingsComponent.debounceTimerValue.getIntSpinner().value
                || fuzzierSettingsService.state.filenameType != fuzzierSettingsComponent.filenameTypeSelector.getFilenameTypeComboBox().selectedItem
                || fuzzierSettingsService.state.boldFilenameWithType != fuzzierSettingsComponent.boldFilenameWithType.getJBCheckBox().isSelected
                || fuzzierSettingsService.state.fontSize != fuzzierSettingsComponent.fontSize.getIntSpinner().value
                || fuzzierSettingsService.state.fileListSpacing != fuzzierSettingsComponent.fileListSpacing.getIntSpinner().value

                || fuzzierSettingsService.state.multiMatch != fuzzierSettingsComponent.multiMatchActive.isSelected
                || fuzzierSettingsService.state.matchWeightPartialPath != fuzzierSettingsComponent.matchWeightPartialPath.value
                || fuzzierSettingsService.state.matchWeightSingleChar != fuzzierSettingsComponent.matchWeightSingleChar.value
                || fuzzierSettingsService.state.matchWeightStreakModifier != fuzzierSettingsComponent.matchWeightStreakModifier.value
    }

    override fun apply() {
        val newList = fuzzierSettingsComponent.exclusionList.getJBTextArea().text
            .split("\n")
            .filter { it.isNotBlank() }
            .ifEmpty { listOf() }

        fuzzierSettingsService.state.exclusionList = newList
        fuzzierSettingsService.state.newTab = fuzzierSettingsComponent.newTabSelect.getJBCheckBox().isSelected
        fuzzierSettingsService.state.debouncePeriod = fuzzierSettingsComponent.debounceTimerValue.getIntSpinner().value as Int
        fuzzierSettingsService.state.filenameType = FilenameType.entries.toTypedArray()[fuzzierSettingsComponent.filenameTypeSelector.getFilenameTypeComboBox().selectedIndex]
        fuzzierSettingsService.state.boldFilenameWithType = fuzzierSettingsComponent.boldFilenameWithType.getJBCheckBox().isSelected
        fuzzierSettingsService.state.fontSize = fuzzierSettingsComponent.fontSize.getIntSpinner().value as Int
        fuzzierSettingsService.state.fileListSpacing = fuzzierSettingsComponent.fileListSpacing.getIntSpinner().value as Int
        
        fuzzierSettingsService.state.multiMatch = fuzzierSettingsComponent.multiMatchActive.isSelected
        fuzzierSettingsService.state.matchWeightPartialPath = fuzzierSettingsComponent.matchWeightPartialPath.value as Int
        fuzzierSettingsService.state.matchWeightSingleChar = fuzzierSettingsComponent.matchWeightSingleChar.value as Int
        fuzzierSettingsService.state.matchWeightStreakModifier = fuzzierSettingsComponent.matchWeightStreakModifier.value as Int
    }
}