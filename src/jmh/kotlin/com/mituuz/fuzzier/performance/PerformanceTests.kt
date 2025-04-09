/*
MIT License

Copyright (c) 2025 Mitja Leino

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
package com.mituuz.fuzzier.performance

import com.mituuz.fuzzier.entities.FuzzyMatchContainer
import com.mituuz.fuzzier.entities.FuzzyMatchContainer.FuzzyScore
import org.openjdk.jmh.annotations.Benchmark

@Suppress("unused")
open class PerformanceTests {
    @Benchmark
    fun allMatchesInFilename() {
        val score = FuzzyScore()
        for (i in 0 until "FuzzyMatchContainerTest.kt".length) {
            score.highlightCharacters.add(i)
        }
        val container = FuzzyMatchContainer(score, "", "FuzzyMatchContainerTest.kt", "")
        container.highlight(container.filename)
    }

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

    @Benchmark
    fun allMatchesInFilenameLegacy() {
        val score = FuzzyScore()
        for (i in 0 until "FuzzyMatchContainerTest.kt".length) {
            score.highlightCharacters.add(i)
        }
        val container = FuzzyMatchContainer(score, "", "FuzzyMatchContainerTest.kt", "")
        container.highlightLegacy(container.filename)
    }

    @Benchmark
    fun sevenMatchesInFilenameLegacy() {
        val score = FuzzyScore()
        score.highlightCharacters.add(0)  // f
        score.highlightCharacters.add(1)  // u
        score.highlightCharacters.add(2)  // z
        score.highlightCharacters.add(3)  // z
        score.highlightCharacters.add(15) // i
        score.highlightCharacters.add(17) // e
        score.highlightCharacters.add(18) // r

        val container = FuzzyMatchContainer(score, "", "FuzzyMatchContainerTest.kt", "")
        container.highlightLegacy(container.filename)
    }

    @Benchmark
    fun threeMatchesInFilenameLegacy() {
        val score = FuzzyScore()
        score.highlightCharacters.add(0)   // f
        score.highlightCharacters.add(15)  // i
        score.highlightCharacters.add(18)  // r

        val container = FuzzyMatchContainer(score, "", "FuzzyMatchContainerTest.kt", "")
        container.highlightLegacy(container.filename)
    }

    @Benchmark
    fun threeMatchesInFilenameInARowLegacy() {
        val score = FuzzyScore()
        score.highlightCharacters.add(0)  // f
        score.highlightCharacters.add(1)  // u
        score.highlightCharacters.add(2)  // z

        val container = FuzzyMatchContainer(score, "", "FuzzyMatchContainerTest.kt", "")
        container.highlightLegacy(container.filename)
    }

    @Benchmark
    fun oneMatchInFilenameLegacy() {
        val score = FuzzyScore()
        score.highlightCharacters.add(0) // F

        val container = FuzzyMatchContainer(score, "", "FuzzyMatchContainerTest.kt", "")
        container.highlightLegacy(container.filename)
    }
}