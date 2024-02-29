package com.mituuz.fuzzier.components

import com.intellij.ui.EditorTextField
import com.intellij.ui.components.JBList
import javax.swing.JPanel

open class FuzzyComponent : JPanel() {
    var fileList = JBList<String?>()
    var searchField = EditorTextField()
    var isDirSelector = false
}