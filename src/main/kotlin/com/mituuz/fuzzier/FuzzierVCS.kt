package com.mituuz.fuzzier

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.mituuz.fuzzier.components.FuzzyFinderComponent
import com.mituuz.fuzzier.entities.FuzzyMatchContainer
import org.apache.commons.lang3.StringUtils
import javax.swing.DefaultListModel
import javax.swing.SwingUtilities


class FuzzierVCS : Fuzzier() {
    override var title: String = "Fuzzy Search (Only VCS Tracked Files)"
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
            val listModel = DefaultListModel<FuzzyMatchContainer>()
            val projectFileIndex = ProjectFileIndex.getInstance(project)
            val changeListManager = ChangeListManager.getInstance(project)
            val projectBasePath = project.basePath

            val stringEvaluator = StringEvaluator(
                fuzzierSettingsService.state.multiMatch,
                fuzzierSettingsService.state.exclusionSet,
                fuzzierSettingsService.state.matchWeightSingleChar,
                fuzzierSettingsService.state.matchWeightStreakModifier,
                fuzzierSettingsService.state.matchWeightPartialPath,
                changeListManager
            )

            val contentIterator =
                projectBasePath?.let { stringEvaluator.getContentIterator(it, searchString, listModel) }

            if (contentIterator != null) {
                projectFileIndex.iterateContent(contentIterator)
            }
            val sortedList = listModel.elements().toList().sortedByDescending { it.getScore() }
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