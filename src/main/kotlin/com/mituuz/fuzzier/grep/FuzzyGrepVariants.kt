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

package com.mituuz.fuzzier.grep

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.mituuz.fuzzier.entities.CaseMode
import com.mituuz.fuzzier.entities.GrepConfig

private object FuzzyGrepTitles {
    const val OPEN_TABS = "Fuzzy Grep Open Tabs"
    const val CURRENT_BUFFER = "Fuzzy Grep Current Buffer"
    const val DEFAULT = "Fuzzy Grep"
}

class FuzzyGrepOpenTabsCI : FuzzyGrep() {
    override fun getGrepConfig(project: Project): GrepConfig {
        val fileEditorManager = FileEditorManager.getInstance(project)
        val openFiles: Array<VirtualFile> = fileEditorManager.openFiles
        val targets = openFiles.map { it.path }

        return GrepConfig(
            targets = targets,
            caseMode = CaseMode.INSENSITIVE,
            title = FuzzyGrepTitles.OPEN_TABS,
        )
    }
}

class FuzzyGrepOpenTabs : FuzzyGrep() {
    override fun getGrepConfig(project: Project): GrepConfig {
        val fileEditorManager = FileEditorManager.getInstance(project)
        val openFiles: Array<VirtualFile> = fileEditorManager.openFiles
        val targets = openFiles.map { it.path }

        return GrepConfig(
            targets = targets,
            caseMode = CaseMode.SENSITIVE,
            title = FuzzyGrepTitles.OPEN_TABS,
        )
    }
}

class FuzzyGrepCurrentBufferCI : FuzzyGrep() {
    override fun getGrepConfig(project: Project): GrepConfig {
        val editor = FileEditorManager.getInstance(project).selectedTextEditor
        val virtualFile: VirtualFile? =
            editor?.let { FileEditorManager.getInstance(project).selectedFiles.firstOrNull() }
        val targets = virtualFile?.path?.let { listOf(it) } ?: emptyList()

        return GrepConfig(
            targets = targets,
            caseMode = CaseMode.INSENSITIVE,
            title = FuzzyGrepTitles.CURRENT_BUFFER,
            supportsSecondaryField = false,
        )
    }
}

class FuzzyGrepCurrentBuffer : FuzzyGrep() {
    override fun getGrepConfig(project: Project): GrepConfig {
        val editor = FileEditorManager.getInstance(project).selectedTextEditor
        val virtualFile: VirtualFile? =
            editor?.let { FileEditorManager.getInstance(project).selectedFiles.firstOrNull() }
        val targets = virtualFile?.path?.let { listOf(it) } ?: emptyList()

        return GrepConfig(
            targets = targets,
            caseMode = CaseMode.SENSITIVE,
            title = FuzzyGrepTitles.CURRENT_BUFFER,
            supportsSecondaryField = false,
        )
    }
}

class FuzzyGrepCI : FuzzyGrep() {
    override fun getGrepConfig(project: Project): GrepConfig {
        return GrepConfig(
            targets = listOf("."),
            caseMode = CaseMode.INSENSITIVE,
            title = FuzzyGrepTitles.DEFAULT,
        )
    }
}
