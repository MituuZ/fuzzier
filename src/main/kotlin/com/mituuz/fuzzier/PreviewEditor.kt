package com.mituuz.fuzzier

import com.intellij.openapi.application.ApplicationManager
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
                if (sourceDocument.text.length > 40000) {
                    loadDocumentInChunks(sourceDocument);
                } else {
                    val length = sourceDocument.textLength
                    println("Handling document with length: $length")
                    ApplicationManager.getApplication().invokeLater {
                        this.document = sourceDocument
                        this.fileType = fileType
                        this.editor?.scrollingModel?.scrollHorizontally(0)
                        this.editor?.scrollingModel?.scrollVertically(0)
                    }
                }
            }
        }
    }

    private fun loadDocumentInChunks(sourceDocument: Document) {
        val length = sourceDocument.textLength
        println("Loading document in chunks with length: $length")
        WriteCommandAction.runWriteCommandAction(project) {
            if (this.document.isWritable) {
                this.document.setText("")
            }
        }

        val reader = BufferedReader(StringReader(sourceDocument.text))
        var line = reader.readLine()
        while (line != null) {
            val stringBuilder = StringBuilder()
            var lineNumber = 0

            while (line != null && lineNumber < 100) {
                stringBuilder.append(line).append("\n")
                lineNumber++
                line = reader.readLine()
            }

            val chunk = stringBuilder.toString()
            ApplicationManager.getApplication().invokeLater {
                WriteCommandAction.runWriteCommandAction(project) {
                    if (this.document.isWritable) {
                        this.document.insertString(this.document.text.length, chunk + "\n")
                    }
                }
            }
        }

        ApplicationManager.getApplication().invokeLater {
            this.fileType = fileType
            this.editor?.scrollingModel?.scrollHorizontally(0)
            this.editor?.scrollingModel?.scrollVertically(0)
        }
    }
}