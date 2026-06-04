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

import com.mituuz.fuzzier.entities.FileAccessData
import com.mituuz.fuzzier.entities.FuzzyContainer
import com.mituuz.fuzzier.entities.FuzzyMatchContainer
import com.mituuz.fuzzier.entities.FuzzyMatchContainer.SerializedMatchContainer.Companion.fromFuzzyMatchContainer
import com.mituuz.fuzzier.settings.FuzzierSettingsService

/**
 * Adds a file to the list of recently searched files while ensuring that the list does not exceed
 * the specified limit, maintains uniqueness, and updates the file metadata cache.
 *
 * @param incomingContainer The container representing the file to be added to the recently
 * searched files list.
 * @param projectState The current state of the project, which holds the recently searched
 * files and related metadata.
 * @param fileListLimit The maximum number of files that can be maintained in the list of
 * recently searched files.
 * @param fileMetadataCacheSize The maximum size allowed for the file metadata cache.
 */
fun addFileToRecentlySearchedFiles(
    incomingContainer: FuzzyContainer,
    projectState: FuzzierSettingsService.State,
    fileListLimit: Int,
    fileMetadataCacheSize: Int,
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

    while (recentFiles.size > fileListLimit - 1) {
        recentFiles.removeAt(recentFiles.size - 1)
    }

    if (incomingContainer is FuzzyMatchContainer) {
        recentFiles.add(incomingContainer)
        projectState.recentlySearchedFiles = recentFiles.map { fromFuzzyMatchContainer(it) }
    }

    projectState.recentFiles = addFileToLRUCache(
        incomingContainer, projectState.recentFiles, fileMetadataCacheSize
    )
}

fun addFileToLRUCache(
    incomingContainer: FuzzyContainer, recentFiles: MutableList<FileAccessData>, maxSize: Int
): MutableList<FileAccessData> {
    val lower = incomingContainer.filePath.lowercase()
    val existingIndex = recentFiles.indexOfFirst { it.filePath.lowercase() == lower }

    val existingEntry = if (existingIndex != -1) recentFiles.removeAt(existingIndex) else null

    val newEntry = FileAccessData(
        filePath = lower,
        accessCount = (existingEntry?.accessCount ?: 0) + 1
    )

    recentFiles.add(0, newEntry)

    while (recentFiles.size > maxSize) {
        recentFiles.removeLast()
    }

    return recentFiles
}
