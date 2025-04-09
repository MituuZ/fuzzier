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

import com.intellij.icons.AllIcons
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.JBIntSpinner
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import com.mituuz.fuzzier.entities.FuzzyContainer.FilenameType
import com.mituuz.fuzzier.settings.FuzzierGlobalSettingsService
import com.mituuz.fuzzier.settings.FuzzierGlobalSettingsService.RecentFilesMode
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JComponent

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

    fun getSearchPositionComboBox(): ComboBox<FuzzierGlobalSettingsService.SearchPosition> {
        @Suppress("UNCHECKED_CAST")
        return component as ComboBox<FuzzierGlobalSettingsService.SearchPosition>
    }

    fun getIntSpinner(index: Int): JBIntSpinner {
        return (component as JPanel).getComponent(index) as JBIntSpinner
    }

    fun getButton(): JButton {
        return component as JButton
    }
}

internal fun FormBuilder.addLabeledComponent(settingsComponent: SettingsComponent): FormBuilder {
    return addLabeledComponent(settingsComponent.title, settingsComponent.component)
}

internal fun FormBuilder.addComponent(settingsComponent: SettingsComponent): FormBuilder {
    if (!settingsComponent.includesSeparateLabel && !settingsComponent.onlyTitle) {
        return addLabeledComponent(settingsComponent.label, settingsComponent.component)
    }

    if (settingsComponent.onlyTitle) {
        return addLabeledComponent(settingsComponent)
    }

    val builder = addComponent(settingsComponent.label)
    return builder.addComponent(settingsComponent.component)
}

internal fun FormBuilder.addComponentFillVertically(settingsComponent: SettingsComponent, inset: Int): FormBuilder {
    val builder = addComponent(settingsComponent.label)
    return builder.addComponentFillVertically(settingsComponent.component, inset)
}
