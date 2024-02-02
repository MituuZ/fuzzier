package com.mituuz.fuzzier.settings

import com.intellij.openapi.components.service
import com.intellij.openapi.options.Configurable
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
        fuzzierSettingsComponent.exclusionList.text = combinedString
        fuzzierSettingsComponent.newTabSelect.isSelected = fuzzierSettingsService.state.newTab
        fuzzierSettingsComponent.debounceTimerValue.value = fuzzierSettingsService.state.debouncePeriod
        fuzzierSettingsComponent.multiMatchActive.isSelected = fuzzierSettingsService.state.multiMatch
        return fuzzierSettingsComponent.jPanel
    }

    override fun isModified(): Boolean {
        return fuzzierSettingsService.state.exclusionList != fuzzierSettingsComponent.exclusionList.text.split("\n")
                || fuzzierSettingsService.state.newTab != fuzzierSettingsComponent.newTabSelect.isSelected
                || fuzzierSettingsService.state.debouncePeriod != fuzzierSettingsComponent.debounceTimerValue.value
                || fuzzierSettingsService.state.multiMatch != fuzzierSettingsComponent.multiMatchActive.isSelected
    }

    override fun apply() {
        val newList = fuzzierSettingsComponent.exclusionList.text
            .split("\n")
            .filter { it.isNotBlank() }
            .ifEmpty { listOf() }

        fuzzierSettingsService.state.exclusionList = newList
        fuzzierSettingsService.state.newTab = fuzzierSettingsComponent.newTabSelect.isSelected
        fuzzierSettingsService.state.debouncePeriod = fuzzierSettingsComponent.debounceTimerValue.value as Int
        fuzzierSettingsService.state.multiMatch = fuzzierSettingsComponent.multiMatchActive.isSelected
    }
}