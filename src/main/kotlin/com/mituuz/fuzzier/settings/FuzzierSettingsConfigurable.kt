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
package com.mituuz.fuzzier.settings

import com.intellij.openapi.components.service
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.mituuz.fuzzier.components.FuzzierSettingsComponent
import javax.swing.JComponent

class FuzzierSettingsConfigurable(project: Project) : Configurable {
    private lateinit var component: FuzzierSettingsComponent
    private var state = project.service<FuzzierSettingsService>().state

    override fun getDisplayName(): String {
        return "Fuzzy File Finder Settings"
    }

    override fun createComponent(): JComponent {
        component = FuzzierSettingsComponent()

        val combinedString = state.exclusionSet.joinToString("\n")
        component.exclusionSet.getJBTextArea().text = combinedString
        component.ignoredCharacters.getJBTextField().text = state.ignoredCharacters
        component.modules.getJBTextArea().text = displayModules(state.modules)
        return component.jPanel
    }

    /**
     * Display the modules in a readable format.
     */
    private fun displayModules(modules: Map<String, String>): String {
        val count = modules.size

        // Check if all values in the modules map are the same
        val uniqueRoots = modules.values.distinct()
        val singleRoot = uniqueRoots.size == 1

        if (singleRoot) {
            return "$count modules in '${uniqueRoots.first()}'"
        }

        return modules.map { "${it.key} in '${it.value}'" }
            .joinToString("\n")
    }

    override fun isModified(): Boolean {
        val newSet = component.exclusionSet.getJBTextArea().text
            .split("\n")
            .filter { it.isNotBlank() }
            .toSet()

        return state.exclusionSet != newSet
                || state.ignoredCharacters != component.ignoredCharacters.getJBTextField().text
    }

    override fun apply() {
        val newSet = component.exclusionSet.getJBTextArea().text
            .split("\n")
            .filter { it.isNotBlank() }
            .toSet()
        state.exclusionSet = newSet as MutableSet<String>
        state.ignoredCharacters = component.ignoredCharacters.getJBTextField().text
    }
}