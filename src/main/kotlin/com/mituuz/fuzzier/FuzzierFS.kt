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
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.findPsiFile
import com.intellij.psi.*
import com.intellij.psi.impl.source.PsiClassReferenceType
import com.mituuz.fuzzier.components.FuzzyFinderComponent
import com.mituuz.fuzzier.entities.FuzzyMatchContainer
import com.mituuz.fuzzier.entities.FuzzyMatchContainer.FuzzyScore
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

    override fun createListeners(project: Project) {
        // Add a listener that updates the contents of the preview pane
        component.fileList.addListSelectionListener { event ->
            if (!event.valueIsAdjusting) {
                if (component.fileList.isEmpty) {
                    ApplicationManager.getApplication().invokeLater {
                        val previewPane = (component as FuzzyFinderComponent).previewPane
                        previewPane.updateFile(EditorFactory.getInstance().createDocument(""))
                    }
                    return@addListSelectionListener
                }
                val selectedValue = component.fileList.selectedValue
                val fileUrl = "file://${selectedValue?.getFileUri()}"

                ProgressManager.getInstance().run(object : Task.Backgroundable(null, "Loading file", false) {
                    override fun run(indicator: ProgressIndicator) {
                        val file = VirtualFileManager.getInstance().findFileByUrl(fileUrl)
                        file?.let {
                            (component as FuzzyFinderComponent).previewPane.updateFile(file)
                        }
                    }
                })
            }
        }
    }

    override fun createInitialView(project: Project) {
        ApplicationManager.getApplication().executeOnPooledThread {
            component.isFs = true
            val fileEditorManager = FileEditorManager.getInstance(project)
            val currentEditor = fileEditorManager.selectedEditor

            val listModel = DefaultListModel<FuzzyMatchContainer>()

            if (currentEditor != null) {
                ApplicationManager.getApplication().runReadAction() {
                    val psiFile: PsiFile? = currentEditor.file.findPsiFile(project)

                    if (psiFile != null) {
                        val uFile = UastFacade.convertElementWithParent(psiFile, UFile::class.java)
                        uFile?.accept(getOpenVisitor(listModel))

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
    }

    private fun getOpenVisitor(listModel: DefaultListModel<FuzzyMatchContainer>): AbstractUastVisitor {
        return object : AbstractUastVisitor() {
            override fun visitClass(node: UClass): Boolean {
                val name = node.name
                val displayString = getTextRepresentation(node, name)
                createStaticContainer(listModel, name, displayString)
                return super.visitClass(node)
            }

            override fun visitMethod(node: UMethod): Boolean {
                val name = node.name
                val displayString = getTextRepresentation(node, name)
                createStaticContainer(listModel, name, displayString)
                return super.visitMethod(node)
            }

            override fun visitVariable(node: UVariable): Boolean {
                val name = node.name
                val displayString = getTextRepresentation(node, name)
                createStaticContainer(listModel, name, displayString)
                return super.visitVariable(node)
            }
        }
    }

    private fun createStaticContainer(listModel: DefaultListModel<FuzzyMatchContainer>,
                                      name: String?, displayString: String?) {
        if (name.isNullOrBlank() || displayString == null) {
            return
        }
        val container = FuzzyMatchContainer(FuzzyScore(), displayString, name)
        listModel.addElement(container)
    }

    private fun getVisitor(listModel: DefaultListModel<FuzzyMatchContainer>,
                           searchString: String = ""): AbstractUastVisitor {
        return object : AbstractUastVisitor() {
            override fun visitClass(node: UClass): Boolean {
                val name = node.name
                val displayString = getTextRepresentation(node, name)
                createContainer(listModel, searchString, displayString, name)
                return super.visitClass(node)
            }

            override fun visitMethod(node: UMethod): Boolean {
                val name = node.name
                val displayString = getTextRepresentation(node, name)
                createContainer(listModel, searchString, displayString, name)
                return super.visitMethod(node)
            }

            override fun visitVariable(node: UVariable): Boolean {
                val name = node.name
                val displayString = getTextRepresentation(node, name)
                createContainer(listModel, searchString, displayString, name)
                return super.visitVariable(node)
            }
        }
    }

    private fun getTextRepresentation(uElement: UElement, name: String?): String? {
        if (name.isNullOrBlank()) {
            return null;
        }
        when (uElement) {
            is UVariable -> {
                val type = uElement.type.presentableText
                return "Variable: $name: $type"
            }
            is UMethod -> {
                val params: List<UParameter> = uElement.uastParameters
                var paramString = ""
                var returnString = ""
                if (params.isNotEmpty()) {
                    paramString = "("
                    for (param: UParameter in params) {
                        paramString = "$paramString${param.name}: ${(param.type as PsiClassReferenceType).name}, "
                    }
                    paramString = paramString.removeSuffix(", ")
                    paramString = "$paramString)"
                } else {
                    paramString = "()"
                }
                val returnType = uElement.returnType
                if (returnType != null && returnType.presentableText != "void") {
                    returnString = ": ${returnType.presentableText}"
                }
                return "Method: $name$paramString$returnString"
            }
            is UClass -> return "Class: $name"
        }
        return null
    }

    private fun createContainer(listModel: DefaultListModel<FuzzyMatchContainer>, searchString: String,
                                displayString: String?, name: String?) {
        if (name.isNullOrBlank() || displayString == null) {
            return;
        }
        val scoreCalculator = ScoreCalculator(searchString)
        val fs = scoreCalculator.calculateScore(name)
        if (fs != null) {
            val container = FuzzyMatchContainer(fs, displayString, name)
            listModel.addElement(container)
        }
    }
}