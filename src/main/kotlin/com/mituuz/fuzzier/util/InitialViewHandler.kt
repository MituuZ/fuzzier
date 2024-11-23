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
                if (listModel.get(i) == null) {
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
                result.addElement(listModel.get(index))
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
                if (listModel.get(i).filePath == fuzzyMatchContainer.filePath) {
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