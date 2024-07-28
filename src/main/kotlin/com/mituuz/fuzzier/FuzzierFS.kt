/*
MIT License

Copyright (c) 2024 Mitja Leino

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/
package com.mituuz.fuzzier

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.findPsiFile
import com.intellij.psi.*
import com.mituuz.fuzzier.entities.FuzzyMatchContainer
import com.mituuz.fuzzier.entities.ScoreCalculator
import org.jetbrains.uast.*
import org.jetbrains.uast.visitor.AbstractUastVisitor
import javax.swing.DefaultListModel

class FuzzierFS : Fuzzier() {
    override var title: String = "Fuzzy Search (File Structure)"
    override fun updateListContents(project: Project, searchString: String) {
        component.isFs = true
        val fileEditorManager = FileEditorManager.getInstance(project)
        val currentEditor = fileEditorManager.selectedEditor

        val listModel = DefaultListModel<FuzzyMatchContainer>()

        if (currentEditor != null) {
            ApplicationManager.getApplication().runReadAction() {
                val psiFile: PsiFile? = currentEditor.file.findPsiFile(project)

                if (psiFile != null) {
                    val uFile = UastFacade.convertElementWithParent(psiFile, UFile::class.java)
                    uFile?.accept(getVisitor(listModel, searchString))

                    ProgressManager.getInstance().run {
                        component.fileList.model = listModel
                        component.fileList.cellRenderer = getCellRenderer()
                        component.fileList.setPaintBusy(false)
                        if (!component.fileList.isEmpty) {
                            component.fileList.setSelectedValue(listModel[0], true)
                        }
                    }
                }
            }
        }
    }

    private fun getVisitor(listModel: DefaultListModel<FuzzyMatchContainer>, searchString: String): AbstractUastVisitor {
        return object : AbstractUastVisitor() {
            override fun visitClass(node: UClass): Boolean {
                val textRange = node.sourcePsi?.textRange
                var offset = ""
                if (textRange != null) {
                    offset = textRange.startOffset.toString()
                }
                val name = node.name
                createContainer(listModel, searchString, "Class", name, offset)
                return super.visitClass(node)
            }

            override fun visitMethod(node: UMethod): Boolean {
                val textRange = node.sourcePsi?.textRange
                var offset = ""
                if (textRange != null) {
                    offset = textRange.startOffset.toString()
                }
                val name = node.name
                createContainer(listModel, searchString, "Method", name, offset)
                return super.visitMethod(node)
            }

            override fun visitVariable(node: UVariable): Boolean {
                val textRange = node.sourcePsi?.textRange
                var offset = ""
                if (textRange != null) {
                    offset = textRange.startOffset.toString()
                }
                val name = node.name
                createContainer(listModel, searchString, "Variable", name, offset)
                return super.visitVariable(node)
            }
        }
    }

    private fun createContainer(listModel: DefaultListModel<FuzzyMatchContainer>, searchString: String,
                                type: String, name: String?, offset: String) {
        if (name.isNullOrBlank() || offset.isBlank()) {
            return
        }
        val scoreCalculator = ScoreCalculator(searchString)
        val fs = scoreCalculator.calculateScore(name)
        if (fs != null) {
            val container = FuzzyMatchContainer(fs, "$type $name at $offset", name, "")
            listModel.addElement(container)
        }
    }
}