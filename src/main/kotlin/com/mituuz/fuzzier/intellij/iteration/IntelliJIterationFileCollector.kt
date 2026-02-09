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

package com.mituuz.fuzzier.intellij.iteration

import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.rootManager
import com.intellij.openapi.roots.FileIndex
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
import com.mituuz.fuzzier.entities.IterationEntry
import com.mituuz.fuzzier.settings.FuzzierSettingsService

class IntelliJIterationFileCollector(val projectState: FuzzierSettingsService.State) : IterationFileCollector {
    override fun collectFiles(
        project: Project,
        shouldContinue: () -> Boolean,
        fileFilter: (VirtualFile) -> Boolean
    ): List<IterationEntry> = buildList {
        val targetIndexes = getTargetIndexes(project)

        for ((fileIndex, moduleName) in targetIndexes) {
            fileIndex.iterateContent { vf ->
                if (!shouldContinue()) return@iterateContent false

                if (fileFilter(vf)) {
                    val iteratorEntry = IterationEntry(vf.name, vf.path, moduleName, vf.isDirectory)
                    add(iteratorEntry)
                }

                true
            }
        }
    }

    private fun getTargetIndexes(project: Project): List<Pair<FileIndex, String>> {
        return if (projectState.isProject) {
            listOf(ProjectFileIndex.getInstance(project) to project.name)
        } else {
            val moduleManager = ModuleManager.getInstance(project)
            moduleManager.modules.map { it.rootManager.fileIndex to it.name }
        }
    }
}