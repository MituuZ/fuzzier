package com.mituuz.fuzzier

import com.intellij.openapi.project.Project

import git4idea.repo.GitRepository

class GitAction : FuzzyAction() {
    override fun updateListContents(project: Project, searchString: String) {
        TODO("Not yet implemented")
        throw NotImplementedError("File finder for git files not yet implemented")
    }
}