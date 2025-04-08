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

import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.FormBuilder
import com.intellij.ui.components.JBTextField
import javax.swing.*
import javax.swing.border.LineBorder

class FuzzierSettingsComponent {
    var jPanel: JPanel

    val exclusionSet = SettingsComponent(JBTextArea(), "File path exclusions",
        """
            Exclusions apply to Fuzzier search and FuzzyMover. One line per one exclusion.<br><br>
            Empty lines are skipped and all files in the project root start with "/"<br><br>
            Supports wildcards (*) for starts with and ends with. Defaults to contains if no wildcards are present.<br><br>
            e.g. "kt" excludes all files/file paths that contain the "kt" string. (main.<strong>kt</strong>, <strong>kt</strong>lin.java)
    """.trimIndent())

    val ignoredCharacters = SettingsComponent(JBTextField(), "Ignored characters",
        """
            Exclude characters from affecting the search. Any character added here will be skipped during the search.<br>
            This could be useful for example when copy pasting similar file paths.<br>
            <strong>Note! </strong>This is case insensitive, everything is considered as lowercase
            <br><br>
            e.g. "%" would transform a search string like "%%%kot%%lin" to "kotlin"
        """.trimIndent(),
        false)

    private val toggleButton = JToggleButton("Show project modules").apply {
        isSelected = false
    }

    val modules = SettingsComponent(JBTextArea().apply { isEditable = false }, "Modules",
        """
            For debugging purposes only. This is a list of all modules in the project.<br>
            If all modules have the same root path, only show the count of modules.
        """.trimIndent(),
        false)

    init {
        setupComponents()
        jPanel = FormBuilder.createFormBuilder()
            .addComponent(JBLabel("<html><strong>General settings</strong></html>"))
            .addComponentFillVertically(exclusionSet, 1)
            .addComponent(ignoredCharacters)
            .addComponent(toggleButton)
            .addComponent(modules)

            .panel
    }

    private fun setupComponents() {
        exclusionSet.component.border = LineBorder(JBColor.BLACK, 1)
        modules.getJBTextArea().isVisible = false
        modules.label.isVisible = false


        toggleButton.addActionListener {
            val expanded = toggleButton.isSelected
            toggleButton.text = if (expanded) "Hide project modules" else "Show project modules"
            modules.getJBTextArea().isVisible = expanded
            modules.label.isVisible = expanded
        }
    }
}