package com.mituuz.fuzzier.components

import com.intellij.ui.EditorTextField
import com.intellij.ui.components.JBList
import com.mituuz.fuzzier.entities.FuzzyMatchContainer
import javax.swing.JPanel

open class FuzzyComponent : JPanel() {
    var fileList = JBList<FuzzyMatchContainer?>()
    var searchField = EditorTextField()
    var isDirSelector = false
}