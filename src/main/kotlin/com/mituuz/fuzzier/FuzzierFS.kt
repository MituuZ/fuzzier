package com.mituuz.fuzzier

import com.intellij.openapi.project.Project

class FuzzierFS : Fuzzier() {
    override var title: String = "Fuzzy Search (File Structure)"
    override fun updateListContents(project: Project, searchString: String) {
        println("Hello from the file struct")
        super.updateListContents(project, searchString)
    }
}