package com.mituuz.fuzzier

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.roots.ContentIterator
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.wm.WindowManager
import javax.swing.DefaultListModel

class Fuzzier : AnAction() {
    private var component = FuzzyFinder()

    override fun actionPerformed(p0: AnActionEvent) {
        // Indicate that we are loading the data
        component.fileList.setPaintBusy(true)

        p0.project?.let {
            val listModel = DefaultListModel<String>()
            val projectFileIndex = ProjectFileIndex.getInstance(it)
            val projectBasePath = it.basePath

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
                        val file = VirtualFileManager.getInstance().findFileByUrl("file://$projectBasePath$selectedValue")

                        file?.let {
                            val document = FileDocumentManager.getInstance().getDocument(it)
                            component.previewPane.text = document?.text ?: "Cannot read file"
                        }
                    }
                }
            }
        }

        val mainWindow = WindowManager.getInstance().getIdeFrame(p0.project)?.component
        mainWindow?.let {
            JBPopupFactory
                    .getInstance()
                    .createComponentPopupBuilder(component, null)
                    .createPopup()
                    .showInCenterOf(it)
        }
    }
}
