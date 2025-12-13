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

package com.mituuz.fuzzier.fileaction

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.rootManager
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.vfs.VirtualFile
import com.mituuz.fuzzier.FuzzyAction
import com.mituuz.fuzzier.entities.IterationEntry
import com.mituuz.fuzzier.intellij.iteration.IntelliJIterationFileCollector
import com.mituuz.fuzzier.intellij.iteration.IterationFileCollector
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.job

abstract class FileAction : FuzzyAction() {
    private var collector: IterationFileCollector = IntelliJIterationFileCollector()

    abstract override fun runAction(
        project: Project,
        actionEvent: AnActionEvent
    )

    abstract override fun createPopup(screenDimensionKey: String): JBPopup

    abstract override fun updateListContents(project: Project, searchString: String)

    abstract fun buildFileFilter(project: Project): (VirtualFile) -> Boolean

    suspend fun collectIterationFiles(project: Project): List<IterationEntry> {
        val ctx = currentCoroutineContext()
        val job = ctx.job

        val indexTargets = if (projectState.isProject) {
            listOf(ProjectFileIndex.getInstance(project) to project.name)
        } else {
            val moduleManager = ModuleManager.getInstance(project)
            moduleManager.modules.map { it.rootManager.fileIndex to it.name }
        }

        return collector.collectFiles(
            targets = indexTargets,
            shouldContinue = { job.isActive },
            fileFilter = buildFileFilter(project)
        )
    }
}