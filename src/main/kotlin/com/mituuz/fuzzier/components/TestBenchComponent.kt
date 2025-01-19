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
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.project.rootManager
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.ui.EditorTextField
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import com.intellij.uiDesigner.core.GridConstraints
import com.intellij.uiDesigner.core.GridLayoutManager
import com.mituuz.fuzzier.entities.FuzzyContainer
import com.mituuz.fuzzier.entities.StringEvaluator
import com.mituuz.fuzzier.entities.FuzzyMatchContainer
import com.mituuz.fuzzier.settings.FuzzierSettingsService
import com.mituuz.fuzzier.util.FuzzierUtil
import org.apache.commons.lang3.StringUtils
import java.awt.Dimension
import java.util.*
import java.util.concurrent.Future
import javax.swing.DefaultListModel
import javax.swing.JPanel
import javax.swing.table.DefaultTableModel
import kotlin.concurrent.schedule

class TestBenchComponent : JPanel() {
    private val columnNames = arrayOf("Filename", "Filepath", "Streak", "MultiMatch", "PartialPath", "Filename", "Total")
    private val table = JBTable()
    private var searchField = EditorTextField()
    private var debounceTimer: TimerTask? = null
    @Volatile
    var currentTask: Future<*>? = null
    private lateinit var liveSettingsComponent: FuzzierGlobalSettingsComponent
    private lateinit var projectState: FuzzierSettingsService.State

    fun fill(settingsComponent: FuzzierGlobalSettingsComponent) {
        val project = ProjectManager.getInstance().openProjects[0]
        projectState = project.service<FuzzierSettingsService>().state

        val fuzzierUtil = FuzzierUtil()
        fuzzierUtil.parseModules(project)

        liveSettingsComponent = settingsComponent
        layout = GridLayoutManager(2, 1)
        val scrollPane = JBScrollPane()
        scrollPane.setViewportView(table)

        add(
            scrollPane,
            GridConstraints(
                0,
                0,
                1,
                1,
                GridConstraints.ANCHOR_CENTER,
                GridConstraints.FILL_BOTH,
                GridConstraints.SIZEPOLICY_CAN_SHRINK or GridConstraints.SIZEPOLICY_WANT_GROW,
                GridConstraints.SIZEPOLICY_CAN_SHRINK or GridConstraints.SIZEPOLICY_WANT_GROW,
                Dimension(-1, 300),
                Dimension(-1, -1),
                null,
                0,
                false
            )
        )
        add(
            searchField,
            GridConstraints(
                1,
                0,
                1,
                1,
                GridConstraints.ANCHOR_WEST,
                GridConstraints.FILL_HORIZONTAL,
                GridConstraints.SIZEPOLICY_WANT_GROW,
                GridConstraints.SIZEPOLICY_FIXED,
                null,
                Dimension(-1, -1),
                null,
                0,
                false
            )
        )

        // Add a listener that updates the search list every time a change is made
        val document = searchField.document

        document.addDocumentListener(object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                debounceTimer?.cancel()
                val debouncePeriod = liveSettingsComponent.debounceTimerValue.getIntSpinner().value as Int
                debounceTimer = Timer().schedule(debouncePeriod.toLong()) {
                    updateListContents(project, searchField.text)
                }
            }
        })
    }

    fun updateListContents(project: Project, searchString: String) {
        if (StringUtils.isBlank(searchString)) {
           ApplicationManager.getApplication().invokeLater {
                table.model = DefaultTableModel()
            }
            return
        }

        val stringEvaluator = StringEvaluator(
            projectState.exclusionSet,
            project.service<FuzzierSettingsService>().state.modules
        )

        currentTask?.takeIf { !it.isDone }?.cancel(true)

        currentTask = ApplicationManager.getApplication().executeOnPooledThread {
            table.setPaintBusy(true)
            val listModel = DefaultListModel<FuzzyContainer>()

            process(project, stringEvaluator, searchString, listModel)

            val sortedList = listModel.elements().toList().sortedByDescending { (it as FuzzyMatchContainer).getScore() }
            val data: Array<Array<Any>> = sortedList.map {
                arrayOf(
                    (it as FuzzyMatchContainer).filename as Any,
                    it.filePath as Any,
                    it.score.streakScore as Any,
                    it.score.multiMatchScore as Any,
                    it.score.partialPathScore as Any,
                    it.score.filenameScore as Any,
                    it.score.getTotalScore() as Any
                )
            }.toTypedArray()

            val tableModel = DefaultTableModel(data, columnNames)
            table.model = tableModel
            table.setPaintBusy(false)
        }
    }

    private fun process(project: Project, stringEvaluator: StringEvaluator, searchString: String, 
                        listModel: DefaultListModel<FuzzyContainer>) {
        val moduleManager = ModuleManager.getInstance(project)
        if (project.service<FuzzierSettingsService>().state.isProject) {
            processProject(project, stringEvaluator, searchString, listModel)
        } else {
            processModules(moduleManager, stringEvaluator, searchString, listModel)
        }
    }

    private fun processProject(project: Project, stringEvaluator: StringEvaluator,
                               searchString: String, listModel: DefaultListModel<FuzzyContainer>) {
        val ss = FuzzierUtil.cleanSearchString(searchString, projectState.ignoredCharacters)
        val contentIterator = stringEvaluator.getContentIterator(project.name, ss, listModel, null)

        val scoreCalculator = stringEvaluator.scoreCalculator
        scoreCalculator.setMultiMatch(liveSettingsComponent.multiMatchActive.getCheckBox().isSelected)
        scoreCalculator.setMatchWeightSingleChar(liveSettingsComponent.matchWeightSingleChar.getIntSpinner().value as Int)
        scoreCalculator.setMatchWeightStreakModifier(liveSettingsComponent.matchWeightStreakModifier.getIntSpinner().value as Int)
        scoreCalculator.setMatchWeightPartialPath(liveSettingsComponent.matchWeightPartialPath.getIntSpinner().value as Int)
        scoreCalculator.setFilenameMatchWeight(liveSettingsComponent.matchWeightFilename.getIntSpinner().value as Int)
        ProjectFileIndex.getInstance(project).iterateContent(contentIterator)
    }

    private fun processModules(moduleManager: ModuleManager, stringEvaluator: StringEvaluator,
                               searchString: String, listModel: DefaultListModel<FuzzyContainer>) {
        for (module in moduleManager.modules) {
            val moduleFileIndex = module.rootManager.fileIndex
            val ss = FuzzierUtil.cleanSearchString(searchString, projectState.ignoredCharacters)
            val contentIterator = stringEvaluator.getContentIterator(module.name, ss, listModel, null)

            val scoreCalculator = stringEvaluator.scoreCalculator
            scoreCalculator.setMultiMatch(liveSettingsComponent.multiMatchActive.getCheckBox().isSelected)
            scoreCalculator.setMatchWeightSingleChar(liveSettingsComponent.matchWeightSingleChar.getIntSpinner().value as Int)
            scoreCalculator.setMatchWeightStreakModifier(liveSettingsComponent.matchWeightStreakModifier.getIntSpinner().value as Int)
            scoreCalculator.setMatchWeightPartialPath(liveSettingsComponent.matchWeightPartialPath.getIntSpinner().value as Int)
            scoreCalculator.setFilenameMatchWeight(liveSettingsComponent.matchWeightFilename.getIntSpinner().value as Int)

            moduleFileIndex.iterateContent(contentIterator)
        }
    }
}