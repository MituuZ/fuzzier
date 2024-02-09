package com.mituuz.fuzzier.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.ui.EditorTextField
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.uiDesigner.core.GridConstraints
import com.intellij.uiDesigner.core.GridLayoutManager
import com.mituuz.fuzzier.StringEvaluator
import com.mituuz.fuzzier.StringEvaluator.FuzzyMatchContainer
import java.awt.Dimension
import java.util.*
import java.util.concurrent.Future
import javax.swing.DefaultListModel
import javax.swing.JPanel
import javax.swing.SwingUtilities
import kotlin.concurrent.schedule

class TestBenchComponent : JPanel() {
    private var fileList = JBList<String?>()
    private var searchField = EditorTextField()
    private var debounceTimer: TimerTask? = null
    @Volatile
    var currentTask: Future<*>? = null
    private lateinit var liveSettingsComponent: FuzzierSettingsComponent

    fun fill(settingsComponent: FuzzierSettingsComponent) {
        liveSettingsComponent = settingsComponent
        layout = GridLayoutManager(2, 1)
        val scrollPane = JBScrollPane()
        scrollPane.setViewportView(fileList)

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
                val debouncePeriod = liveSettingsComponent.debounceTimerValue.value as Int
                debounceTimer = Timer().schedule(debouncePeriod.toLong()) {
                    updateListContents(project, searchField.text)
                }
            }
        })
    }

    fun updateListContents(project: Project, searchString: String) {
        val newList = liveSettingsComponent.exclusionList.text
            .split("\n")
            .filter { it.isNotBlank() }
            .ifEmpty { listOf() }
        val stringEvaluator = StringEvaluator(liveSettingsComponent.multiMatchActive.isSelected,
            newList, liveSettingsComponent.matchWeightSingleChar.value as Int,
            liveSettingsComponent.matchWeightStreakModifier.value as Int,
            liveSettingsComponent.matchWeightPartialPath.value as Int)

        currentTask?.takeIf { !it.isDone }?.cancel(true)

        currentTask = ApplicationManager.getApplication().executeOnPooledThread {
            fileList.setPaintBusy(true)
            val listModel = DefaultListModel<FuzzyMatchContainer>()
            val projectFileIndex = ProjectFileIndex.getInstance(project)
            val projectBasePath = project.basePath

            val contentIterator = projectBasePath?.let { stringEvaluator.getContentIterator(it, searchString, listModel) }

            if (contentIterator != null) {
                projectFileIndex.iterateContent(contentIterator)
            }
            val sortedList = listModel.elements().toList().sortedByDescending { it.score }
            val valModel = DefaultListModel<String>()
            sortedList.forEach { valModel.addElement(it.string) }

            SwingUtilities.invokeLater {
                fileList.model = valModel
                fileList.setPaintBusy(false)
                if (!fileList.isEmpty) {
                    fileList.setSelectedValue(valModel[0], true)
                }
            }
        }
    }
}