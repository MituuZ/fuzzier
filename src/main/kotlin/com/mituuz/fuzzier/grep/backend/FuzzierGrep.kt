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
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.rootManager
import com.intellij.openapi.roots.FileIndex
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiSearchHelper
import com.intellij.util.Processor
import com.mituuz.fuzzier.entities.CaseMode
import com.mituuz.fuzzier.entities.FuzzyContainer
import com.mituuz.fuzzier.entities.GrepConfig
import com.mituuz.fuzzier.entities.RowContainer
import com.mituuz.fuzzier.runner.CommandRunner
import kotlinx.coroutines.*
import javax.swing.DefaultListModel

object FuzzierGrep : BackendStrategy {
    override val name = "fuzzier"

    override fun buildCommand(
        grepConfig: GrepConfig,
        searchString: String,
        secondarySearchString: String?
    ): List<String> = emptyList()

    override suspend fun handleSearch(
        grepConfig: GrepConfig,
        searchString: String,
        secondarySearchString: String?,
        commandRunner: CommandRunner,
        listModel: DefaultListModel<FuzzyContainer>,
        projectBasePath: String,
        project: Project?,
        fileFilter: (VirtualFile) -> Boolean
    ) {
        if (project == null) return

        val fileCollectionStart = System.nanoTime()

        val files = collectFiles(searchString, fileFilter, project, grepConfig)

        val fileCollectionEnd = System.nanoTime()
        val fileCollectionDuration = (fileCollectionEnd - fileCollectionStart) / 1_000_000.0
        println("File collection took $fileCollectionDuration ms")

        println("Searching from ${files.size} files with ${grepConfig.caseMode} case mode and $searchString")

        var count = 0
        val batchSize = 20
        val currentBatch = mutableListOf<FuzzyContainer>()

        val fileProcessingStart = System.nanoTime()

        for (file in files) {
            currentCoroutineContext().ensureActive()

            val matches = withContext(Dispatchers.IO) {
                val content = VfsUtil.loadText(file)
                val lines = content.lines()
                val fileMatches = mutableListOf<RowContainer>()

                for ((index, line) in lines.withIndex()) {
                    currentCoroutineContext().ensureActive()

                    val found = if (grepConfig.caseMode == CaseMode.INSENSITIVE) {
                        line.contains(searchString, ignoreCase = true)
                    } else {
                        line.contains(searchString)
                    }

                    if (found) {
                        fileMatches.add(
                            RowContainer(
                                file.path,
                                projectBasePath,
                                file.name,
                                index,
                                line.trim()
                            )
                        )
                    }
                }
                fileMatches
            }

            for (match in matches) {
                currentCoroutineContext().ensureActive()

                currentBatch.add(match)
                count++

                if (currentBatch.size >= batchSize) {
                    val toAdd = currentBatch.toList()
                    currentBatch.clear()
                    withContext(Dispatchers.Main) {
                        listModel.addAll(toAdd)
                    }
                }

                if (count >= 1000) break
            }

            if (count >= 1000) break
        }

        val fileProcessingEnd = System.nanoTime()

        val fileProcessingDuration = (fileProcessingEnd - fileProcessingStart) / 1_000_000.0
        println("File processing took $fileProcessingDuration ms")

        if (currentBatch.isNotEmpty()) {
            withContext(Dispatchers.Main) {
                listModel.addAll(currentBatch)
            }
        }
    }

    private suspend fun collectFiles(
        searchString: String,
        fileFilter: (VirtualFile) -> Boolean,
        project: Project,
        grepConfig: GrepConfig
    ): List<VirtualFile> {
        var files = listOf<VirtualFile>()
        val trimmedSearch = searchString.trim()

        if (false) {// (trimmedSearch.contains(" ")) {
            // Case B: Long String (Length >= 3) - PsiSearchHelper
            val searchString = trimmedSearch.substringBeforeLast(" ")

            if (searchString.isNotEmpty()) {
                ReadAction.run<Throwable> {
                    val helper = PsiSearchHelper.getInstance(project)
                    helper.processAllFilesWithWord(
                        searchString,
                        GlobalSearchScope.projectScope(project),
                        Processor { psiFile ->
                            psiFile.virtualFile?.let { vf ->
                                if (fileFilter(vf)) {
                                    // files.add(vf)
                                }
                            }
                            true
                        },
                        grepConfig.caseMode == CaseMode.SENSITIVE
                    )
                }
            }
        } else {
            // Case A: Short String (Length < 3) - Linear iteration
            files = collectIterationFiles(project)
        }

        return files
    }

    suspend fun collectIterationFiles(project: Project): List<VirtualFile> {
        val ctx = currentCoroutineContext()
        val job = ctx.job

        val indexTargets = if (false) { // (projectState.isProject) {
            listOf(ProjectFileIndex.getInstance(project) to project.name)
        } else {
            val moduleManager = ModuleManager.getInstance(project)
            moduleManager.modules.map { it.rootManager.fileIndex to it.name }
        }

        return collectFiles(
            targets = indexTargets,
            shouldContinue = { job.isActive },
            fileFilter = buildFileFilter(project)
        )
    }

    private fun buildFileFilter(project: Project): (VirtualFile) -> Boolean {
        val clm = ChangeListManager.getInstance(project)
        return { vf -> !vf.isDirectory && !clm.isIgnoredFile(vf) && !vf.fileType.isBinary }
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