package com.mituuz.fuzzier.util

import com.intellij.openapi.components.service
import com.mituuz.fuzzier.entities.FuzzyMatchContainer
import com.mituuz.fuzzier.settings.FuzzierSettingsService
import java.util.*
import javax.swing.DefaultListModel

class FuzzierUtil {
    private var settingsState = service<FuzzierSettingsService>().state
    private var listLimit: Int = settingsState.fileListLimit

    /**
     * Process all the elements in the listModel with a priority queue to limit the size
     * and keeping the data sorted at all times
     *
     * Priority queue's size is limit + 1 to prevent any resizing
     * Only add entries to the queue if they have larger score than the minimum in the queue
     *
     * @param listModel to limit and sort
     * @param isDirSort defaults to false, enables using different sort for directories
     *
     * @return a sorted and sized list model
     */
    fun sortAndLimit(listModel: DefaultListModel<FuzzyMatchContainer>, isDirSort: Boolean = false): DefaultListModel<FuzzyMatchContainer> {
        val priorityQueue = PriorityQueue(listLimit + 1, compareBy(FuzzyMatchContainer::getScore))

        var minimumScore = -1
        listModel.elements().toList().forEach {
            if (it.getScore() > minimumScore) {
                priorityQueue.add(it)
                if (priorityQueue.size > listLimit) {
                    minimumScore = priorityQueue.remove().getScore()
                }
            }
        }

        val result = DefaultListModel<FuzzyMatchContainer>()
        if (isDirSort && settingsState.prioritizeShorterDirPaths) {
            result.addAll(priorityQueue.toList().sortedByDescending { it.getScoreWithDirLength() })
        } else {
            result.addAll(priorityQueue.toList().sortedByDescending { it.getScore() })
        }

        return result
    }

    fun setListLimit(limit: Int) {
        listLimit = limit
    }
}