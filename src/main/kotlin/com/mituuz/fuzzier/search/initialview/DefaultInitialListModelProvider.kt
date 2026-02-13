/*
 *  MIT License
 *
 *  Copyright (c) 2025 Mitja Leino
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package com.mituuz.fuzzier.search.initialview

import com.intellij.openapi.fileEditor.impl.EditorHistoryManager
import com.intellij.openapi.project.Project
import com.mituuz.fuzzier.entities.FuzzyContainer
import com.mituuz.fuzzier.entities.OrderedContainer
import com.mituuz.fuzzier.settings.FuzzierGlobalSettingsService
import com.mituuz.fuzzier.settings.FuzzierSettingsService
import com.mituuz.fuzzier.util.FuzzierUtil
import javax.swing.DefaultListModel

class DefaultInitialListModelProvider(
    val project: Project,
    val globalState: FuzzierGlobalSettingsService.State,
    val projectState: FuzzierSettingsService.State,
) : InitialListModelProvider {
    override fun invoke(): DefaultListModel<FuzzyContainer> {
        return when (globalState.recentFilesMode) {
            FuzzierGlobalSettingsService.RecentFilesMode.RECENT_PROJECT_FILES -> {
                getRecentProjectFiles(project)
            }

            FuzzierGlobalSettingsService.RecentFilesMode.RECENTLY_SEARCHED_FILES -> {
                getRecentlySearchedFiles()
            }

            else -> {
                DefaultListModel()
            }
        }
    }

    fun getRecentProjectFiles(
        project: Project,
    ): DefaultListModel<FuzzyContainer> {
        val editorHistoryManager = EditorHistoryManager.getInstance(project)
        val editorHistory = editorHistoryManager.fileList
        val listModel = DefaultListModel<FuzzyContainer>()
        val limit = globalState.fileListLimit

        // Start from the end of editor history (most recent file)
        var i = editorHistory.size - 1
        while (i >= 0 && listModel.size() < limit) {
            val file = editorHistory[i]
            val filePathAndModule = FuzzierUtil.extractModulePath(file.path, projectState.modules)
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

    fun getRecentlySearchedFiles(): DefaultListModel<FuzzyContainer> {
        val result = DefaultListModel<FuzzyContainer>()
        projectState.getRecentlySearchedFilesAsFuzzyMatchContainer()
            .elements()
            .toList()
            .filterNotNull()
            .reversed()
            .let {
                result.addAll(it)
            }
        return result
    }
}