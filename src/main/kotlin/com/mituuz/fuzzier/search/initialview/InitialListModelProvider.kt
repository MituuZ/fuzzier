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

import com.mituuz.fuzzier.entities.FuzzyContainer
import com.mituuz.fuzzier.entities.FuzzyMatchContainer
import com.mituuz.fuzzier.settings.FuzzierGlobalSettingsService
import com.mituuz.fuzzier.settings.FuzzierSettingsService
import javax.swing.DefaultListModel

interface InitialListModelProvider {
    fun buildInitialView(): DefaultListModel<FuzzyContainer>

    companion object {
        fun addFileToRecentlySearchedFiles(
            fuzzyContainer: FuzzyContainer,
            projectState: FuzzierSettingsService.State,
            globalState: FuzzierGlobalSettingsService.State
        ) {
            val listModel: DefaultListModel<FuzzyMatchContainer> =
                projectState.getRecentlySearchedFilesAsFuzzyMatchContainer()

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
                projectState.recentlySearchedFiles =
                    FuzzyMatchContainer.SerializedMatchContainer.fromListModel(listModel)
            }

        }
    }

}