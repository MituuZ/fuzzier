package com.mituuz.fuzzier

import com.intellij.ide.structureView.StructureViewModel
import com.intellij.ide.structureView.TreeBasedStructureViewBuilder
import com.intellij.ide.structureView.impl.common.PsiTreeElementBase
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.ex.util.EditorUtil

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.findPsiFile
import com.intellij.psi.*
import com.mituuz.fuzzier.entities.FuzzyMatchContainer
import com.mituuz.fuzzier.entities.FuzzyMatchContainer.FuzzyScore
import org.jetbrains.uast.*
import org.jetbrains.uast.visitor.AbstractUastVisitor
import javax.swing.DefaultListModel

class FuzzierFS : Fuzzier() {
    override var title: String = "Fuzzy Search (File Structure)"
    override fun updateListContents(project: Project, searchString: String) {
        println("Hello from the file struct")
        val fileEditorManager = FileEditorManager.getInstance(project)
        val currentEditor = fileEditorManager.selectedEditor

        val listModel = DefaultListModel<FuzzyMatchContainer>()

        if (currentEditor != null) {
            ApplicationManager.getApplication().runReadAction() {
                val psiFile: PsiFile? = currentEditor.file.findPsiFile(project)

                if (psiFile != null) {
                    val uFile = UastFacade.convertElementWithParent(psiFile, UFile::class.java)
                    uFile?.accept(getVisitor(listModel))

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

    private fun getVisitor(listModel: DefaultListModel<FuzzyMatchContainer>): AbstractUastVisitor {
        return object : AbstractUastVisitor() {
            override fun visitClass(node: UClass): Boolean {
                val textRange = node.sourcePsi?.textRange
                var offset = ""
                if (textRange != null) {
                    offset = textRange.startOffset.toString()
                }
                val name = node.name
                createContainer(listModel, "Class", name, offset)
                return super.visitClass(node)
            }

            override fun visitMethod(node: UMethod): Boolean {
                val textRange = node.sourcePsi?.textRange
                var offset = ""
                if (textRange != null) {
                    offset = textRange.startOffset.toString()
                }
                val name = node.name
                createContainer(listModel, "Method", name, offset)
                return super.visitMethod(node)
            }

            override fun visitVariable(node: UVariable): Boolean {
                val textRange = node.sourcePsi?.textRange
                var offset = ""
                if (textRange != null) {
                    offset = textRange.startOffset.toString()
                }
                val name = node.name
                createContainer(listModel, "Variable", name, offset)
                return super.visitVariable(node)
            }
        }
    }

    private fun createContainer(listModel: DefaultListModel<FuzzyMatchContainer>, type: String, name: String?,
                                offset: String) {
        if (name.isNullOrBlank() || offset.isBlank()) {
            return
        }
        val fs = FuzzyScore()
        val container = FuzzyMatchContainer(fs, "$type $name at $offset", name, "")
        listModel.addElement(container)
    }
}