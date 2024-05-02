package com.mituuz.fuzzier

import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.ChangeListManager

/**
 * Search for only VCS tracked files
 */
class FuzzierVCS : Fuzzier() {
    override var title: String = "Fuzzy Search (Only VCS Tracked Files)"
    override fun updateListContents(project: Project, searchString: String) {
        changeListManager = ChangeListManager.getInstance(project)
        super.updateListContents(project, searchString)
    }
}