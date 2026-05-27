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
import com.mituuz.fuzzier.entities.FuzzyMatchContainer.SerializedMatchContainer.Companion.fromFuzzyMatchContainer
import com.mituuz.fuzzier.settings.FuzzierGlobalSettingsService
import com.mituuz.fuzzier.settings.FuzzierSettingsService

/**
 * Adds a file to the list of recently searched files, maintaining the limit set by global settings.
 * If the file already exists in the list, it is removed and re-added to ensure it appears as the most recent.
 *
 * @param incomingContainer The container holding information about the file being added to the list.
 * @param projectState The state of the current project, containing the project's recently searched files.
 * @param globalState The global settings state, including configuration like the file list limit.
 */
fun addFileToRecentlySearchedFiles(
    incomingContainer: FuzzyContainer,
    projectState: FuzzierSettingsService.State,
    globalState: FuzzierGlobalSettingsService.State
) {
    val recentFiles: MutableList<FuzzyMatchContainer> =
        projectState.recentlySearchedFiles?.mapNotNull { it.toFuzzyMatchContainer() }?.toMutableList()
            ?: mutableListOf()

    var i = 0
    while (i < recentFiles.size) {
        if (recentFiles[i].filePath == incomingContainer.filePath) {
            recentFiles.removeAt(i)
        } else {
            i++
        }
    }

    while (recentFiles.size > globalState.fileListLimit - 1) {
        recentFiles.removeAt(recentFiles.size - 1)
    }

    if (incomingContainer is FuzzyMatchContainer) {
        recentFiles.add(incomingContainer)

        projectState.recentlySearchedFiles = recentFiles.map { fromFuzzyMatchContainer(it) }
    }
}
