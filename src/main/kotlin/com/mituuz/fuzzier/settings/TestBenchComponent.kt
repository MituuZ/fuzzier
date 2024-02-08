package com.mituuz.fuzzier.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.roots.ContentIterator
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorTextField
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.uiDesigner.core.GridConstraints
import com.intellij.uiDesigner.core.GridLayoutManager
import org.apache.commons.lang3.StringUtils
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
    private var fuzzierSettingsService = service<FuzzierSettingsService>()
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

    data class FuzzyMatchContainer(val score: Int, val string: String)

    fun updateListContents(project: Project, searchString: String) {
        currentTask?.takeIf { !it.isDone }?.cancel(true)

        currentTask = ApplicationManager.getApplication().executeOnPooledThread {
            fileList.setPaintBusy(true)
            val listModel = DefaultListModel<FuzzyMatchContainer>()
            val projectFileIndex = ProjectFileIndex.getInstance(project)
            val projectBasePath = project.basePath

            val contentIterator = projectBasePath?.let { getContentIterator(it, searchString, listModel) }

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

    fun getContentIterator(projectBasePath: String, searchString: String, listModel: DefaultListModel<FuzzyMatchContainer>): ContentIterator {
        return ContentIterator { file: VirtualFile ->
            if (!file.isDirectory) {
                val filePath = projectBasePath.let { it1 -> file.path.removePrefix(it1) }
                if (isExcluded(filePath)) {
                    return@ContentIterator true
                }
                if (filePath.isNotBlank()) {
                    val fuzzyMatchContainer = fuzzyContainsCaseInsensitive(filePath, searchString)
                    if (fuzzyMatchContainer != null) {
                        listModel.addElement(fuzzyMatchContainer)
                    }
                }
            }
            true
        }
    }

    private fun isExcluded(filePath: String): Boolean {
        val exclusionList = fuzzierSettingsService.state.exclusionList
        for (e in exclusionList) {
            when {
                e.startsWith("*") -> {
                    if (filePath.endsWith(e.substring(1))) {
                        return true
                    }
                }
                e.endsWith("*") -> {
                    if (filePath.startsWith(e.substring(0, e.length - 1))) {
                        return true
                    }
                }
                filePath.contains(e) -> {
                    return true
                }
            }
        }
        return false
    }

    fun fuzzyContainsCaseInsensitive(filePath: String, searchString: String): FuzzyMatchContainer? {
        if (searchString.isBlank()) {
            return FuzzyMatchContainer(0, filePath)
        }
        if (searchString.length > filePath.length) {
            return null
        }

        val lowerFilePath: String = filePath.lowercase()
        val lowerSearchString: String = searchString.lowercase()
        return getFuzzyMatch(lowerFilePath, lowerSearchString, filePath)
    }

    private fun getFuzzyMatch(lowerFilePath: String, lowerSearchString: String, filePath: String): FuzzyMatchContainer? {
        var score = 0
        for (s in StringUtils.split(lowerSearchString, " ")) {
            score += processSearchString(s, lowerFilePath) ?: return null
        }
        return FuzzyMatchContainer(score, filePath)
    }

    private fun processSearchString(s: String, lowerFilePath: String): Int? {
        var longestStreak = 0
        var streak = 0
        var score = 0.0
        var prevIndex = -10
        var match = 0
        for (searchStringIndex in s.indices) {
            if (lowerFilePath.length - searchStringIndex < s.length - searchStringIndex) {
                return null
            }

            var found = -1
            // Always process the whole file path for each character, assuming they're found
            for (filePathIndex in lowerFilePath.indices) {
                if (s[searchStringIndex] == lowerFilePath[filePathIndex]) {
                    match++
                    // Always increase score when finding a match
                    if (liveSettingsComponent.multiMatchActive.isSelected) {
                        score += liveSettingsComponent.matchWeightSingleChar.value as Int / 10.0
                    }
                    // Only check streak and update the found variable, if the current match index is greater than the previous
                    if (found == -1 && filePathIndex > prevIndex) {
                        // TODO: Does not work quite correct when handling a search string where a char is found first and then again for a multi match
                        // If the index is one greater than the previous chars, increment streak and update the longest streak
                        if (prevIndex + 1 == filePathIndex) {
                            streak++
                            if (streak > longestStreak) {
                                longestStreak = streak
                            }
                        } else {
                            streak = 1
                        }
                        // Save the first found index of a new character
                        prevIndex = filePathIndex
                        if (!liveSettingsComponent.multiMatchActive.isSelected) {
                            // Set found to verify a match and exit the loop
                            found = filePathIndex
                            continue;
                        }
                    }
                    // When multiMatch is disabled, setting found exits the loop. Only set found for multiMatch
                    if (liveSettingsComponent.multiMatchActive.isSelected) {
                        found = filePathIndex
                    }
                }
            }

            // Check that the character was found and that it was found after the previous characters index
            // Here we could skip once to broaden the search
            if (found == -1 || prevIndex > found) {
                return null
            }
        }

        // If we get to here, all characters were found and have been accounted for in the score
        return calculateScore(streak, longestStreak, lowerFilePath, s, score)
    }

    private fun calculateScore(streak: Int, longestStreak: Int, lowerFilePath: String, lowerSearchString: String, stringComparisonScore: Double): Int {
        var score: Double = if (streak > longestStreak) {
            (liveSettingsComponent.matchWeightStreakModifier.value as Int / 10.0) * streak + stringComparisonScore
        } else {
            (liveSettingsComponent.matchWeightStreakModifier.value as Int / 10.0) * longestStreak + stringComparisonScore
        }

        StringUtils.split(lowerFilePath, "/.").forEach {
            if (it == lowerSearchString) {
                score += liveSettingsComponent.matchWeightPartialPath.value as Int
            }
        }

        return score.toInt()
    }
}