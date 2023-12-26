package com.mituuz.fuzzier

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.roots.ContentIterator
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.vfs.VirtualFile
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
        }

        component.fileList.setPaintBusy(false)

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
