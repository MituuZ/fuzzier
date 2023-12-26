package com.mituuz.fuzzier

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.roots.ContentIterator
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.wm.WindowManager
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.AbstractAction
import javax.swing.DefaultListModel
import javax.swing.KeyStroke

class Fuzzier : AnAction() {
    private var component = FuzzyFinder()
    private var popup: JBPopup? = null

    override fun actionPerformed(p0: AnActionEvent) {
        // Indicate that we are loading the data
        component.fileList.setPaintBusy(true)
        component.searchField.isFocusable = true

        p0.project?.let { project ->
            val listModel = DefaultListModel<String>()
            val projectFileIndex = ProjectFileIndex.getInstance(project)
            val projectBasePath = project.basePath

            val contentIterator = ContentIterator { file: VirtualFile ->
                if (!file.isDirectory) {
                    val filePath = projectBasePath?.let { it1 -> file.path.removePrefix(it1) }
                    if (!filePath.isNullOrBlank()) {
                        listModel.addElement(filePath)
                    }
                }
                true
            }

            projectFileIndex.iterateContent(contentIterator)
            component.fileList?.model = listModel

            // Data has been loaded
            component.fileList.setPaintBusy(false)

            // Add listener that updates the contents of the preview pane
            component.fileList.addListSelectionListener { event ->
                run {
                    if (!event.valueIsAdjusting) {
                        val selectedValue = component.fileList.selectedValue
                        val file =
                            VirtualFileManager.getInstance().findFileByUrl("file://$projectBasePath$selectedValue")

                        file?.let {
                            val document = FileDocumentManager.getInstance().getDocument(it)
                            component.previewPane.text = document?.text ?: "Cannot read file"
                        }
                    }
                }
            }

            // Add MouseListener for double-click
            component.fileList.addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) {
                    if (e.clickCount == 2) {
                        val selectedValue = component.fileList.selectedValue
                        val virtualFile = VirtualFileManager.getInstance().findFileByUrl("file://$projectBasePath$selectedValue")

                        // Open the file in the editor
                        virtualFile?.let {
                            popup?.cancel()
                            FileEditorManager.getInstance(project).openFile(it, true)
                        }
                    }
                }
            })

        }

        val mainWindow = WindowManager.getInstance().getIdeFrame(p0.project)?.component
        mainWindow?.let {
            popup = JBPopupFactory
                    .getInstance()
                    .createComponentPopupBuilder(component, component.searchField)
                    .createPopup();
            popup!!.showInCenterOf(it)
        }
    }
}
