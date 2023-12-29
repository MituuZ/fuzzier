package com.mituuz.fuzzier

import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorTextField

class PreviewEditor(project: Project?, virtualFile: VirtualFile?) : EditorTextField(
    virtualFile?.let { FileDocumentManager.getInstance().getDocument(it) },
    project,
    virtualFile?.let { FileTypeManager.getInstance().getFileTypeByFile(it) }
) {

    override fun createEditor(): EditorEx {
        val editor = super.createEditor()
        editor.settings.isLineNumbersShown = true
        editor.setVerticalScrollbarVisible(true)
        editor.isOneLineMode = false
        editor.isViewer = true
        return editor
    }

    fun updateFile(virtualFile: VirtualFile?) {
        if (virtualFile != null) {
            this.document = virtualFile.let { FileDocumentManager.getInstance().getDocument(it) }!!
        }
        this.fileType = virtualFile?.let { FileTypeManager.getInstance().getFileTypeByFile(it) };
    }
}