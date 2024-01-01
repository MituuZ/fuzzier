package com.mituuz.fuzzier

import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorTextField

class PreviewEditor(project: Project?, virtualFile: VirtualFile?) : EditorTextField(
    virtualFile?.let { FileDocumentManager.getInstance().getDocument(it) },
    project,
    getDefaultFileType()
) {

    companion object {
        fun getDefaultFileType(): FileType {
            return PlainTextFileType.INSTANCE
        }
    }

    override fun createEditor(): EditorEx {
        val editor = super.createEditor()
        editor.settings.isLineNumbersShown = true
        editor.setVerticalScrollbarVisible(true)
        editor.isOneLineMode = false
        editor.isViewer = true
        editor.foldingModel.isFoldingEnabled = false

        val globalScheme = EditorColorsManager.getInstance().globalScheme
        this.font = globalScheme.getFont(null)
        return editor
    }

    fun updateFile(document: Document) {
        this.document = document
        this.fileType = PlainTextFileType.INSTANCE
    }

    fun updateFile(document: Document, virtualFile: VirtualFile?) {
        this.document = document
        this.fileType = virtualFile?.let { FileTypeManager.getInstance().getFileTypeByFile(it) };
    }
}