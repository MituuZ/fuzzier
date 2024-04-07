package com.mituuz.fuzzier.util

import com.intellij.openapi.components.service
import com.mituuz.fuzzier.entities.FuzzyMatchContainer
import com.mituuz.fuzzier.settings.FuzzierSettingsService
import java.util.*
import javax.swing.DefaultListModel

class FuzzierUtil {
    private var listLimit: Int = service<FuzzierSettingsService>().state.fileListLimit

    /**
     * Process all the elements in the listModel with a priority queue to limit the size
     * and keeping the data sorted at all times
     *
     * Priority queue's size is limit + 1 to prevent any resizing
     * Only add entries to the queue if they have larger score than the minimum in the queue
     *
     * @return a sorted and sized list model
     */
    fun sortAndLimit(listModel: DefaultListModel<FuzzyMatchContainer>): DefaultListModel<FuzzyMatchContainer> {
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
        result.addAll(priorityQueue.toList().sortedByDescending { it.getScore() })

        return result
    }

    fun setListLimit(limit: Int) {
        listLimit = limit
    }
}