package com.mituuz.fuzzier.components

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.ui.EditorTextField
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.table.JBTable
import com.intellij.uiDesigner.core.GridConstraints
import com.intellij.uiDesigner.core.GridLayoutManager
import com.mituuz.fuzzier.StringEvaluator
import com.mituuz.fuzzier.entities.FuzzyMatchContainer
import org.apache.commons.lang3.StringUtils
import java.awt.Dimension
import java.util.*
import java.util.concurrent.Future
import javax.swing.DefaultListModel
import javax.swing.JPanel
import javax.swing.SwingUtilities
import javax.swing.table.DefaultTableModel
import kotlin.concurrent.schedule

class TestBenchComponent : JPanel() {
    private val columnNames = arrayOf("Filename", "Filepath", "Streak", "MultiMatch", "PartialPath", "Filename", "Total")
    private val table = JBTable()
    private var searchField = EditorTextField()
    private var debounceTimer: TimerTask? = null
    @Volatile
    var currentTask: Future<*>? = null
    private lateinit var liveSettingsComponent: FuzzierSettingsComponent

    fun fill(settingsComponent: FuzzierSettingsComponent) {
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

        val project = ProjectManager.getInstance().openProjects[0]
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
            SwingUtilities.invokeLater {
                table.model = DefaultTableModel()
            }
            return
        }

        val newSet = (liveSettingsComponent.exclusionSet.component as JBTextArea).text
            .split("\n")
            .filter { it.isNotBlank() }
            .toSet()
        val stringEvaluator = StringEvaluator(liveSettingsComponent.multiMatchActive.getCheckBox().isSelected,
            newSet, liveSettingsComponent.matchWeightSingleChar.getIntSpinner().value as Int,
            liveSettingsComponent.matchWeightStreakModifier.getIntSpinner().value as Int,
            liveSettingsComponent.matchWeightPartialPath.getIntSpinner().value as Int)

        currentTask?.takeIf { !it.isDone }?.cancel(true)

        currentTask = ApplicationManager.getApplication().executeOnPooledThread {
            table.setPaintBusy(true)
            val listModel = DefaultListModel<FuzzyMatchContainer>()

            val projectFileIndex = ProjectFileIndex.getInstance(project)
            val projectBasePath = project.basePath

            val contentIterator = projectBasePath?.let { stringEvaluator.getContentIterator(it, searchString, listModel) }
            
            val scoreCalculator = stringEvaluator.scoreCalculator
            scoreCalculator.setMultiMatch(liveSettingsComponent.multiMatchActive.getCheckBox().isSelected)
            scoreCalculator.setMatchWeightSingleChar(liveSettingsComponent.matchWeightSingleChar.getIntSpinner().value as Int)
            scoreCalculator.setMatchWeightStreakModifier(liveSettingsComponent.matchWeightStreakModifier.getIntSpinner().value as Int)
            scoreCalculator.setMatchWeightPartialPath(liveSettingsComponent.matchWeightPartialPath.getIntSpinner().value as Int)

            if (contentIterator != null) {
                projectFileIndex.iterateContent(contentIterator)
            }
            val sortedList = listModel.elements().toList().sortedByDescending { it.getScore() }
            val data = sortedList.map {
                arrayOf(it.filename, it.filePath, it.score.streakScore, it.score.multiMatchScore,
                    it.score.partialPathScore, it.score.filenameScore, it.score.getTotalScore())
            }.toTypedArray()

            val tableModel = DefaultTableModel(data, columnNames)

            SwingUtilities.invokeLater {
                table.model = tableModel
                table.setPaintBusy(false)
            }
        }
    }
}