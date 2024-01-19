package com.mituuz.fuzzier

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.WriteCommandAction
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
import java.io.BufferedReader
import java.io.StringReader

class PreviewEditor(project: Project?) : EditorTextField(
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
        editor.setVerticalScrollbarVisible(true)
        editor.setHorizontalScrollbarVisible(true)
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

    fun updateFile(virtualFile: VirtualFile?) {
        ApplicationManager.getApplication().executeOnPooledThread {
            val sourceDocument = ApplicationManager.getApplication().runReadAction<Document?> {
                virtualFile?.let { FileDocumentManager.getInstance().getDocument(virtualFile) }
            }
            val fileType = virtualFile?.let { FileTypeManager.getInstance().getFileTypeByFile(virtualFile) }
            if (sourceDocument != null) {
            val reader = BufferedReader(StringReader(sourceDocument.text))
                var lineNumber = 0
                var line = reader.readLine()
                while (line != null && lineNumber < 2000) {
                    ApplicationManager.getApplication().invokeLater {
                        WriteCommandAction.runWriteCommandAction(project) {
                            this.document.insertString(this.document.text.length, line + "\n")
                            line = reader.readLine()
                            lineNumber++
                        }
                    }
                }

                ApplicationManager.getApplication().invokeLater {
                    this.fileType = fileType
                    this.editor?.scrollingModel?.scrollHorizontally(0)
                }
            }
        }
    }
}