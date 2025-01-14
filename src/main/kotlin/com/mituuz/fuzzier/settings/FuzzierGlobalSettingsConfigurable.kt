package com.mituuz.fuzzier.settings

import com.intellij.openapi.components.service
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.util.NlsContexts
import com.mituuz.fuzzier.components.FuzzierGlobalSettingsComponent
import javax.swing.JComponent

class FuzzierGlobalSettingsConfigurable : Configurable {
    private lateinit var component: FuzzierGlobalSettingsComponent
    private var state = service<FuzzierGlobalSettingsService>().state

    override fun getDisplayName(): @NlsContexts.ConfigurableName String? {
        return "Fuzzier Global Settings"
    }

    override fun createComponent(): JComponent? {
        component = FuzzierGlobalSettingsComponent()
        return component.jPanel
    }

    override fun isModified(): Boolean {
        return false
    }

    override fun apply() {

    }
}