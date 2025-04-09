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

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.annotations.OptionTag
import com.mituuz.fuzzier.entities.FuzzyMatchContainer
import javax.swing.DefaultListModel

@State(
    name = "com.mituuz.fuzzier.FuzzierSettings",
    storages = [Storage("FuzzierSettings.xml")],
    reloadable = true
)
@Service(Service.Level.PROJECT)
class FuzzierSettingsService : PersistentStateComponent<FuzzierSettingsService.State> {
    class State {
        var modules: Map<String, String> = HashMap()
        var isProject = false
        @OptionTag(converter = FuzzyMatchContainer.SerializedMatchContainerConverter::class)
        var recentlySearchedFiles: DefaultListModel<FuzzyMatchContainer.SerializedMatchContainer>? = DefaultListModel()

        var exclusionSet: Set<String> = setOf("/.idea/*", "/.git/*", "/target/*", "/build/*", "/.gradle/*", "/.run/*")
        var ignoredCharacters: String = ""

        fun getRecentlySearchedFilesAsFuzzyMatchContainer(): DefaultListModel<FuzzyMatchContainer> {
            val list = recentlySearchedFiles ?: DefaultListModel()
            return FuzzyMatchContainer.SerializedMatchContainer.toListModel(list)
        }
    }

    private var state = State()

    override fun getState(): State {
        return this.state
    }

    override fun loadState(p0: State) {
        this.state = p0
    }
}