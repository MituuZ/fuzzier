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
package com.mituuz.fuzzier.util

import com.intellij.openapi.fileEditor.impl.EditorHistoryManager
import com.intellij.openapi.project.Project
import com.mituuz.fuzzier.entities.FuzzyContainer
import com.mituuz.fuzzier.entities.FuzzyMatchContainer
import com.mituuz.fuzzier.entities.OrderedContainer
import com.mituuz.fuzzier.settings.FuzzierGlobalSettingsService
import com.mituuz.fuzzier.settings.FuzzierSettingsService
import javax.swing.DefaultListModel

class InitialViewHandler {
    companion object {
        fun getRecentProjectFiles(
            globalState: FuzzierGlobalSettingsService.State, fuzzierUtil: FuzzierUtil,
            editorHistoryManager: EditorHistoryManager,
            project: Project
        ): DefaultListModel<FuzzyContainer> {
            val editorHistory = editorHistoryManager.fileList
            val listModel = DefaultListModel<FuzzyContainer>()
            val limit = globalState.fileListLimit

            // Start from the end of editor history (most recent file)
            var i = editorHistory.size - 1
            while (i >= 0 && listModel.size() < limit) {
                val file = editorHistory[i]
                val filePathAndModule = fuzzierUtil.extractModulePath(file.path, project)
                // Don't add files that do not have a module path in the project
                if (filePathAndModule.second == "") {
                    i--
                    continue
                }
                val orderedContainer = OrderedContainer(
                    filePathAndModule.first, filePathAndModule.second, file.name
                )
                listModel.addElement(orderedContainer)
                i--
            }

            return listModel
        }

        fun getRecentlySearchedFiles(projectState: FuzzierSettingsService.State): DefaultListModel<FuzzyContainer> {
            var listModel = projectState.getRecentlySearchedFilesAsFuzzyMatchContainer()

            var i = 0
            while (i < listModel.size) {
                if (listModel[i] == null) {
                    listModel.remove(i)
                } else {
                    i++
                }
            }

            // Reverse the list to show the most recent searches first
            var result = DefaultListModel<FuzzyContainer>()

            var j = 0
            while (j < listModel.size) {
                val index = listModel.size - j - 1
                result.addElement(listModel[index])
                j++
            }

            return result
        }

        fun addFileToRecentlySearchedFiles(fuzzyContainer: FuzzyContainer, projectState: FuzzierSettingsService.State,
                                           globalState: FuzzierGlobalSettingsService.State) {
            var listModel: DefaultListModel<FuzzyMatchContainer> = projectState.getRecentlySearchedFilesAsFuzzyMatchContainer()

            var i = 0
            while (i < listModel.size) {
                if (listModel[i].filePath == fuzzyContainer.filePath) {
                    listModel.remove(i)
                } else {
                    i++
                }
            }

            while (listModel.size > globalState.fileListLimit - 1) {
                listModel.remove(listModel.size - 1)
            }

            if (fuzzyContainer is FuzzyMatchContainer) {
                listModel.addElement(fuzzyContainer)
                projectState.recentlySearchedFiles = FuzzyMatchContainer.SerializedMatchContainer.fromListModel(listModel)
            }
        }
    }
}