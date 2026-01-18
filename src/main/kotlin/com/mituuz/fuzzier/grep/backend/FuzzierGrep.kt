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

package com.mituuz.fuzzier.grep.backend

import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.components.service
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.rootManager
import com.intellij.openapi.roots.FileIndex
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiSearchHelper
import com.intellij.openapi.components.service
import com.intellij.util.Processor
import com.mituuz.fuzzier.entities.CaseMode
import com.mituuz.fuzzier.entities.FuzzyContainer
import com.mituuz.fuzzier.entities.GrepConfig
import com.mituuz.fuzzier.entities.RowContainer
import com.mituuz.fuzzier.runner.CommandRunner
import com.mituuz.fuzzier.settings.FuzzierGlobalSettingsService
import com.mituuz.fuzzier.settings.FuzzierSettingsService
import com.mituuz.fuzzier.util.FuzzierUtil
import kotlinx.coroutines.*
import javax.swing.DefaultListModel

object FuzzierGrep : BackendStrategy {
    override val name = "fuzzier"
    private val fuzzierUtil = FuzzierUtil()
    private val searchMatcher = SearchMatcher()

    override suspend fun handleSearch(
        grepConfig: GrepConfig,
        searchString: String,
        secondarySearchString: String?,
        commandRunner: CommandRunner,
        listModel: DefaultListModel<FuzzyContainer>,
        projectBasePath: String,
        project: Project,
        fileFilter: (VirtualFile) -> Boolean
    ) {
        val files = grepConfig.targets ?: collectFiles(searchString, fileFilter, project, grepConfig)

        val maxResults = service<FuzzierGlobalSettingsService>().state.fileListLimit
        val batcher = ResultBatcher<FuzzyContainer>()

        for (file in files) {
            currentCoroutineContext().ensureActive()

            val matches = withContext(Dispatchers.IO) {
                try {
                    val content = VfsUtil.loadText(file)
                    val lines = content.lines()
                    val fileMatches = mutableListOf<RowContainer>()

                    for ((index, line) in lines.withIndex()) {
                        currentCoroutineContext().ensureActive()

                        val found = searchMatcher.matchesLine(line, searchString, grepConfig.caseMode)

                        if (found) {
                            val (filePath, basePath) = fuzzierUtil.extractModulePath(file.path, project)
                            fileMatches.add(
                                RowContainer(
                                    filePath,
                                    basePath,
                                    file.name,
                                    index,
                                    line.trim(),
                                    virtualFile = file
                                )
                            )
                        }
                    }
                    fileMatches
                } catch (_: Exception) {
                    // Skip files that cannot be read (permissions, encoding issues, etc.)
                    emptyList()
                }
            }

            for (match in matches) {
                currentCoroutineContext().ensureActive()

                val batch = batcher.add(match)
                if (batch != null) {
                    withContext(Dispatchers.Main) {
                        listModel.addAll(batch)
                    }
                }

                if (batcher.getCount() >= maxResults) break
            }

            if (batcher.getCount() >= maxResults) break
        }

        if (batcher.hasRemaining()) {
            withContext(Dispatchers.Main) {
                listModel.addAll(batcher.getRemaining())
            }
        }
    }

    private suspend fun collectFiles(
        searchString: String,
        fileFilter: (VirtualFile) -> Boolean,
        project: Project,
        grepConfig: GrepConfig
    ): List<VirtualFile> {
        val files = mutableListOf<VirtualFile>()
        val firstCompleteWord = searchMatcher.extractFirstCompleteWord(searchString)

        if (firstCompleteWord != null) {
            ReadAction.run<Throwable> {
                val helper = PsiSearchHelper.getInstance(project)
                helper.processAllFilesWithWord(
                    firstCompleteWord,
                    GlobalSearchScope.projectScope(project),
                    Processor { psiFile ->
                        psiFile.virtualFile?.let { vf ->
                            if (fileFilter(vf)) {
                                files.add(vf)
                            }
                        }
                        true
                    },
                    grepConfig.caseMode == CaseMode.SENSITIVE
                )
            }
        } else {
            val ctx = currentCoroutineContext()
            val job = ctx.job
            val projectState = project.service<FuzzierSettingsService>().state

            val indexTargets = if (projectState.isProject) {
                listOf(ProjectFileIndex.getInstance(project) to project.name)
            } else {
                val moduleManager = ModuleManager.getInstance(project)
                moduleManager.modules.map { it.rootManager.fileIndex to it.name }
            }

            return collectFiles(
                targets = indexTargets,
                shouldContinue = { job.isActive },
                fileFilter = fileFilter
            )
        }

        return files
    }

    private fun collectFiles(
        targets: List<Pair<FileIndex, String>>,
        shouldContinue: () -> Boolean,
        fileFilter: (VirtualFile) -> Boolean
    ): List<VirtualFile> = buildList {
        for ((fileIndex, _) in targets) {
            fileIndex.iterateContent { vf ->
                if (!shouldContinue()) return@iterateContent false

                if (fileFilter(vf)) {
                    add(vf)
                }

                true
            }
        }
    }
}