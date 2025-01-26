package com.mituuz.fuzzier.performance

import com.mituuz.fuzzier.entities.FuzzyMatchContainer
import com.mituuz.fuzzier.entities.FuzzyMatchContainer.FuzzyScore
import org.openjdk.jmh.annotations.Benchmark

open class PerformanceTests {
    @Benchmark
    fun sevenMatchesInFilename() {
        val score = FuzzyScore()
        score.highlightCharacters.add(0)  // f
        score.highlightCharacters.add(1)  // u
        score.highlightCharacters.add(2)  // z
        score.highlightCharacters.add(3)  // z
        score.highlightCharacters.add(15) // i
        score.highlightCharacters.add(17) // e
        score.highlightCharacters.add(18) // r

        val container = FuzzyMatchContainer(score, "", "FuzzyMatchContainerTest.kt", "")
        container.highlight(container.filename)
    }

    @Benchmark
    fun threeMatchesInFilename() {
        val score = FuzzyScore()
        score.highlightCharacters.add(0)   // f
        score.highlightCharacters.add(15)  // i
        score.highlightCharacters.add(18)  // r

        val container = FuzzyMatchContainer(score, "", "FuzzyMatchContainerTest.kt", "")
        container.highlight(container.filename)
    }

    @Benchmark
    fun threeMatchesInFilenameInARow() {
        val score = FuzzyScore()
        score.highlightCharacters.add(0)  // f
        score.highlightCharacters.add(1)  // u
        score.highlightCharacters.add(2)  // z

        val container = FuzzyMatchContainer(score, "", "FuzzyMatchContainerTest.kt", "")
        container.highlight(container.filename)
    }

    @Benchmark
    fun oneMatchInFilename() {
        val score = FuzzyScore()
        score.highlightCharacters.add(0) // F

        val container = FuzzyMatchContainer(score, "", "FuzzyMatchContainerTest.kt", "")
        container.highlight(container.filename)
    }
}