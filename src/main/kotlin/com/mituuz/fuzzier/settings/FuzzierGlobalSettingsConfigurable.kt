package com.mituuz.fuzzier.settings

import com.intellij.openapi.components.service
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.util.NlsContexts
import com.mituuz.fuzzier.components.FuzzierGlobalSettingsComponent
import com.mituuz.fuzzier.entities.FuzzyContainer.FilenameType
import com.mituuz.fuzzier.settings.FuzzierGlobalSettingsService.RecentFilesMode
import javax.swing.JComponent

class FuzzierGlobalSettingsConfigurable : Configurable {
    private lateinit var component: FuzzierGlobalSettingsComponent
    private var state = service<FuzzierGlobalSettingsService>().state

    override fun getDisplayName(): @NlsContexts.ConfigurableName String? {
        return "Fuzzier Global Settings"
    }

    override fun createComponent(): JComponent? {
        component = FuzzierGlobalSettingsComponent()
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
        return state.newTab != component.newTabSelect.getCheckBox().isSelected
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