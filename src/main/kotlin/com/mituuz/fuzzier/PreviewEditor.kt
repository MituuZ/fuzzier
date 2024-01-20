package com.mituuz.fuzzier

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorTextField
import kotlin.math.min

class PreviewEditor(project: Project?) : EditorTextField(
    project,
    getDefaultFileType()
) {
    private val toBeContinued: String = "\n\n\n--- End of Fuzzier Preview ---\n--- Open file to see full content ---\n\n\n"
    private val fileCutOff: Int = 70000

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

            ApplicationManager.getApplication().invokeLater {
                if (sourceDocument != null) {
                    val endIndex = min(sourceDocument.textLength, fileCutOff)
                    val previewText = sourceDocument.getText(TextRange(0, endIndex))
                    WriteCommandAction.runWriteCommandAction(project) {
                        if (endIndex >= fileCutOff) {
                            // Create a new document which can be modified without changing the original file
                            // Only gets partial highlighting
                            this.document = EditorFactory.getInstance().createDocument(previewText)
                            this.document.insertString(endIndex, toBeContinued)
                        } else {
                            // Use the actual document to enable full highlighting in the preview
                            this.document = sourceDocument
                        }
                        ApplicationManager.getApplication().invokeLater {
                            editor?.scrollingModel?.run {
                                scrollHorizontally(0)
                                scrollVertically(0)
                            }
                        }
                    }
                }
                this.fileType = fileType
            }
        }
    }
}