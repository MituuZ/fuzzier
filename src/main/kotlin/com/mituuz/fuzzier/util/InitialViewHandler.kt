/*
MIT License

Copyright (c) 2024 Mitja Leino

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
package com.mituuz.fuzzier.util

import com.intellij.openapi.fileEditor.impl.EditorHistoryManager
import com.mituuz.fuzzier.entities.FuzzyMatchContainer
import com.mituuz.fuzzier.settings.FuzzierSettingsService
import javax.swing.DefaultListModel

class InitialViewHandler {
    companion object {
        fun getRecentProjectFiles(
            fuzzierSettingsService: FuzzierSettingsService, fuzzierUtil: FuzzierUtil,
            editorHistoryManager: EditorHistoryManager
        ): DefaultListModel<FuzzyMatchContainer> {
            val editorHistory = editorHistoryManager.fileList
            val listModel = DefaultListModel<FuzzyMatchContainer>()
            val limit = fuzzierSettingsService.state.fileListLimit

            // Start from the end of editor history (most recent file)
            var i = editorHistory.size - 1
            while (i >= 0 && listModel.size() < limit) {
                val file = editorHistory[i]
                val filePathAndModule = fuzzierUtil.extractModulePath(file.path)
                // Don't add files that do not have a module path in the project
                if (filePathAndModule.second == "") {
                    i--
                    continue
                }
                val fuzzyMatchContainer = FuzzyMatchContainer.createOrderedContainer(
                    i, filePathAndModule.first, filePathAndModule.second, file.name
                )
                listModel.addElement(fuzzyMatchContainer)
                i--
            }

            return listModel
        }

        fun getRecentlySearchedFiles(fuzzierSettingsService: FuzzierSettingsService): DefaultListModel<FuzzyMatchContainer> {
            var listModel = fuzzierSettingsService.state.recentlySearchedFiles

            if (listModel == null) {
                listModel = DefaultListModel<FuzzyMatchContainer>()
                fuzzierSettingsService.state.recentlySearchedFiles = listModel
            }

            var i = 0
            while (i < listModel.size) {
                if (listModel[i] == null) {
                    listModel.remove(i)
                } else {
                    i++
                }
            }

            // Reverse the list to show the most recent searches first
            var result = DefaultListModel<FuzzyMatchContainer>()

            var j = 0
            while (j < listModel.size) {
                val index = listModel.size - j - 1
                result.addElement(listModel[index])
                j++
            }

            return result
        }

        fun addFileToRecentlySearchedFiles(fuzzyMatchContainer: FuzzyMatchContainer, fuzzierSettingsService: FuzzierSettingsService) {
            var listModel: DefaultListModel<FuzzyMatchContainer>? = fuzzierSettingsService.state.recentlySearchedFiles

            if (listModel == null) {
                listModel = DefaultListModel<FuzzyMatchContainer>()
                fuzzierSettingsService.state.recentlySearchedFiles = listModel
            }

            var i = 0
            while (i < listModel.size) {
                if (listModel[i].filePath == fuzzyMatchContainer.filePath) {
                    listModel.remove(i)
                } else {
                    i++
                }
            }

            while (listModel.size > fuzzierSettingsService.state.fileListLimit - 1) {
                listModel.remove(listModel.size - 1)
            }

            listModel.addElement(fuzzyMatchContainer)
        }
    }
}