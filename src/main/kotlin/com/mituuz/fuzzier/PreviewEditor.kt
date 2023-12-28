package com.mituuz.fuzzier

import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.ui.EditorTextField

class PreviewEditor(string: String, project: Project?, fileType: FileType?) : EditorTextField(
    string,
    project,
    fileType,
) {

    override fun createEditor(): EditorEx {
        val editor = super.createEditor()
        editor.settings.isLineNumbersShown = true
        editor.setVerticalScrollbarVisible(true)
        editor.isOneLineMode = false
        editor.isViewer = true
        return editor
    }
}