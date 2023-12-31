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
        return fuzzierSettingsComponent.jPanel
    }

    override fun isModified(): Boolean {
        return fuzzierSettingsService.state.exclusionList != fuzzierSettingsComponent.exclusionList.text.split("\n")
    }

    override fun apply() {
        val newList = fuzzierSettingsComponent.exclusionList.text
            .split("\n")
            .filter { it.isNotBlank() }
            .ifEmpty { listOf() }
        fuzzierSettingsService.state.exclusionList = newList
    }
}