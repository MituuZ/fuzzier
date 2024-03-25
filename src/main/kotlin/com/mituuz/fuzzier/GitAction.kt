package com.mituuz.fuzzier

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.mituuz.fuzzier.components.FuzzyFinderComponent
import git4idea.GitUtil
import org.apache.commons.lang3.StringUtils
import javax.swing.DefaultListModel
import javax.swing.SwingUtilities


class GitAction : Fuzzier() {
    override fun updateListContents(project: Project, searchString: String) {
        if (StringUtils.isBlank(searchString)) {
            SwingUtilities.invokeLater {
                component.fileList.model = DefaultListModel()
                defaultDoc?.let { (component as FuzzyFinderComponent).previewPane.updateFile(it) }
            }
            return
        }

        currentTask?.takeIf { !it.isDone }?.cancel(true)
        currentTask = ApplicationManager.getApplication().executeOnPooledThread {
            component.fileList.setPaintBusy(true)
            val listModel = DefaultListModel<StringEvaluator.FuzzyMatchContainer>()
            val projectFileIndex = ProjectFileIndex.getInstance(project)
            val projectBasePath = project.basePath

            val repositoryManager = GitUtil.getRepositoryManager(project)
            val gitExclusions = mutableSetOf<String>()
            for (repository in repositoryManager.repositories) {
                val repoGitExclusions = repository.untrackedFilesHolder.ignoredFilePaths.toSet().map {
                    it.virtualFile?.path?.split("/")?.last() ?: ""
                }
                gitExclusions.addAll(repoGitExclusions)
            }

            fuzzierSettingsService.state.exclusionSet.addAll(gitExclusions)
            val stringEvaluator = StringEvaluator(
                fuzzierSettingsService.state.multiMatch,
                fuzzierSettingsService.state.exclusionSet,
                fuzzierSettingsService.state.matchWeightSingleChar,
                fuzzierSettingsService.state.matchWeightStreakModifier,
                fuzzierSettingsService.state.matchWeightPartialPath
            )

            val contentIterator =
                projectBasePath?.let { stringEvaluator.getContentIterator(it, searchString, listModel) }

            if (contentIterator != null) {
                projectFileIndex.iterateContent(contentIterator)
            }
            val sortedList = listModel.elements().toList().sortedByDescending { it.score }
            listModel.clear()
            sortedList.forEach { listModel.addElement(it) }

            SwingUtilities.invokeLater {
                component.fileList.model = listModel
                component.fileList.cellRenderer = getCellRenderer()
                component.fileList.setPaintBusy(false)
                if (!component.fileList.isEmpty) {
                    component.fileList.setSelectedValue(listModel[0], true)
                }
            }
        }
    }
}