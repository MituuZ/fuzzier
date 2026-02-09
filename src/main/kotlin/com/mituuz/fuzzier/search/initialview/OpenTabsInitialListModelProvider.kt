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

import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.mituuz.fuzzier.entities.FuzzyContainer
import com.mituuz.fuzzier.entities.OrderedContainer
import com.mituuz.fuzzier.util.FuzzierUtil
import kotlinx.html.InputType
import javax.swing.DefaultListModel

class OpenTabsInitialListModelProvider(
) : InitialListModelProvider {
    override fun buildInitialView(project: Project): DefaultListModel<FuzzyContainer> {
        val fileEditorManager = FileEditorManager.getInstance(project)
        val listModel = DefaultListModel<FuzzyContainer>()

        ReadAction.run<Throwable> {
            for (vf in fileEditorManager.openFiles) {
                if (!vf.isDirectory) {
                    val filePathAndModule = FuzzierUtil.extractModulePath(vf.path, project)
                    // Don't add files that do not have a module path in the project
                    if (filePathAndModule.second == "") {
                        continue
                    }
                    val orderedContainer = OrderedContainer(
                        filePathAndModule.first, filePathAndModule.second, InputType.file.name
                    )
                    listModel.add(0, orderedContainer)
                }
            }
        }

        return listModel
    }
}