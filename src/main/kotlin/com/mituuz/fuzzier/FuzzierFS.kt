package com.mituuz.fuzzier

import com.intellij.ide.structureView.StructureViewModel
import com.intellij.ide.structureView.TreeBasedStructureViewBuilder
import com.intellij.ide.structureView.impl.common.PsiTreeElementBase
// Don't remove me
import com.intellij.ide.util.FileStructurePopup
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.ex.util.EditorUtil

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.findPsiFile
import com.intellij.psi.*
import org.jetbrains.uast.*
import org.jetbrains.uast.visitor.AbstractUastVisitor

class FuzzierFS : Fuzzier() {
    override var title: String = "Fuzzy Search (File Structure)"
    override fun updateListContents(project: Project, searchString: String) {
        println("Hello from the file struct")
        val projectFileIndex = ProjectFileIndex.getInstance(project)
        val fileEditorManager = FileEditorManager.getInstance(project)
        val currentEditor = fileEditorManager.selectedEditor
        // val fs = FileStructurePopup

        if (currentEditor != null) {
            ApplicationManager.getApplication().runReadAction() {
                val psiFile: PsiFile? = currentEditor.file.findPsiFile(project)

                if (psiFile != null) {
                    val uFile = UastFacade.convertElementWithParent(psiFile, UFile::class.java)
                    uFile?.accept(object : AbstractUastVisitor() {
                        override fun visitClass(node: UClass): Boolean {
                            val textRange = node.textRange
                            var offset = ""
                            if (textRange != null) {
                                offset = textRange.startOffset.toString()
                            }
                            println("Found class: ${node.name} at offset: ${offset}")
                            return super.visitClass(node)
                        }

                        override fun visitMethod(node: UMethod): Boolean {
                            val textRange = node.textRange
                            var offset = ""
                            if (textRange != null) {
                                offset = textRange.startOffset.toString()
                            }
                            println("Found method: ${node.name} at offset: ${offset}")
                            return super.visitMethod(node)
                        }

                        override fun visitVariable(node: UVariable): Boolean {
                            val textRange = node.textRange
                            var offset = ""
                            if (textRange != null) {
                                offset = textRange.startOffset.toString()
                            }
                            println("Found variable: ${node.name} at offset: ${offset}")
                            return super.visitVariable(node)
                        }
                    })
                }

                val builder: TreeBasedStructureViewBuilder = currentEditor.structureViewBuilder as TreeBasedStructureViewBuilder
                val treeModel: StructureViewModel = builder.createStructureViewModel(EditorUtil.getEditorEx(currentEditor));

                for (element in treeModel.root.children[0].children) {
                    val asd = element as PsiTreeElementBase<*>
                    val el = asd.element
                    if (el is PsiNamedElement) {
                        // println("Found element with name: ${el.name}");
                    }
                }
                // JavaRecursiveElementVisitor

//                PsiElement
//
//                PsiClass.getMethods();
//                val ps: PsiLocalVariable

                if (psiFile != null) {
                    val rec = Visitor()
                    // psiFile.accept(rec)
                    // PsiTreeUtil.collectElementsOfType()
//                    val elements = PsiTreeUtil.getChildrenOfAnyType(psiFile, KtClass::class.java, KtNamedFunction::class.java, KtProperty::class.java)
//                    for (element in elements) {
//                        when (element) {
//                            is KtClass -> println("Found class: ${element.name}")
//                            is KtNamedFunction -> println("Found function: ${element.name}")
//                            is KtProperty -> println("Found property: ${element.name}")
//                        }
//                    }
                    // Here's a KtNamedFunction when looking at FuzzyAction
                    val elements = psiFile.children[6].children[2].children // as KtNamedFunction
                    for (element in elements) {
                        val asd = element.toUElement();
                        // println(asd)
                    }
                }
                psiFile?.processChildren { c ->
                    c.processChildren {
                        // println("Found another child $c")
                        true
                    }
                    // println("Found child $c")
                    true
                }
                // println(psiFile)
            }
            // PsiTreeUtil.findChildOfAnyType(currentEditor.file.getPsiFile(project), )

        }

        // com.intellij.psi.SyntaxTraverser could be used for more powerful tree navigation
        super.updateListContents(project, searchString)
    }

    class Visitor : PsiRecursiveElementWalkingVisitor() {
        override fun visitFile(file: PsiFile) {
            super.visitFile(file)
        }

        override fun visitElement(element: PsiElement) {
            println(element)
            super.visitElement(element)
        }
    }
}