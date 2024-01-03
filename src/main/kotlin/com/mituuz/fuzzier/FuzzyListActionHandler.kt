package com.mituuz.fuzzier

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.EditorActionHandler

class FuzzyListActionHandler(private val fuzzier: Fuzzier, private val isUp: Boolean) : EditorActionHandler() {
    override fun doExecute(editor: Editor, caret: Caret?, dataContext: DataContext?) {
        if (isUp) {
            fuzzier.moveListUp()
        } else {
            fuzzier.moveListDown()
        }

        super.doExecute(editor, caret, dataContext)
    }
}