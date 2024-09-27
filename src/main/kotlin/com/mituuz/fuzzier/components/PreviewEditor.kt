/*
MIT License

Copyright (c) 2024 Mitja Leino

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/
package com.mituuz.fuzzier.components

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.ScrollType
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
import com.mituuz.fuzzier.settings.FuzzierSettingsService
import kotlin.math.min

class PreviewEditor(project: Project?) : EditorTextField(
    project,
    getDefaultFileType()
) {
    private val toBeContinued: String = "\n\n\n--- End of Fuzzier Preview ---\n--- Open file to see full content ---\n\n\n"
    private val fileCutOff: Int = 70000
    private val settingsState = service<FuzzierSettingsService>().state

    companion object {
        fun getDefaultFileType(): FileType {
            return PlainTextFileType.INSTANCE
        }
    }

    override fun createEditor(): EditorEx {
        val editor = super.createEditor() // TODO: Exception in thread com.intellij.openapi.diagnostic.RuntimeExceptionWithAttachments: Access is allowed from Event Dispatch Thread (EDT) only; see https://jb.gg/ij-platform-threading for details editor.setVerticalScrollbarVisible(true)
        /*
            at com.mituuz.fuzzier.components.PreviewEditor.updateFile(PreviewEditor.kt:78)
            at com.mituuz.fuzzier.Fuzzier.createListeners$lambda$12(Fuzzier.kt:234)
            at com.mituuz.fuzzier.FuzzierFS.updateListContents$lambda$1(FuzzierFS.kt:39)
         */
        editor.setVerticalScrollbarVisible(true)
        editor.setHorizontalScrollbarVisible(true)
        editor.isOneLineMode = false
        editor.isViewer = true
        editor.foldingModel.isFoldingEnabled = false

        val globalScheme = EditorColorsManager.getInstance().globalScheme
        this.font = globalScheme.getFont(null)

        val previewFontSize = settingsState.previewFontSize
        if (previewFontSize != 0) {
            this.font = this.font.deriveFont(previewFontSize.toFloat())
        }

        return editor
    }

    fun updateFile(document: Document) {
        this.document = document
        this.fileType = PlainTextFileType.INSTANCE
    }

    fun updateFile(virtualFile: VirtualFile?, offset: Int? = null) {
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
                            if (offset != null) {
                                val caret = editor?.caretModel?.primaryCaret
                                if (caret != null) {
                                    caret.moveToOffset(offset)
                                    val logicalPosition = caret.logicalPosition
                                    editor?.scrollingModel?.scrollTo(logicalPosition, ScrollType.CENTER)
                                }
                            }
                        }
                    }
                } else {
                    this.document = EditorFactory.getInstance().createDocument("Cannot preview file")
                }
                this.fileType = fileType
            }
        }
    }
}