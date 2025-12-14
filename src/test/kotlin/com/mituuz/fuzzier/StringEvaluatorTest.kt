/*
 *  MIT License
 *
 *  Copyright (c) 2025 Mitja Leino
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */
package com.mituuz.fuzzier

import com.intellij.testFramework.TestApplicationManager
import com.mituuz.fuzzier.entities.IterationEntry
import com.mituuz.fuzzier.entities.MatchConfig
import com.mituuz.fuzzier.entities.StringEvaluator
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class StringEvaluatorTest {
    @Suppress("unused")
    private var testApplicationManager: TestApplicationManager = TestApplicationManager.getInstance()

    private val moduleName = "mod1"
    private val moduleBasePath = "/m1/src"

    private fun evaluate(filePaths: List<String>, exclusionList: Set<String>): List<String> {
        val evaluator = StringEvaluator(exclusionList, mapOf(moduleName to moduleBasePath))

        return filePaths.mapNotNull { fp ->
            // Build absolute path under a fake module root so that removePrefix(moduleBasePath) works like in production
            val relativeToSrc = fp.removePrefix("src") // e.g., "src/a/b.kt" -> "/a/b.kt"
            val fullPath = "/m1/src$relativeToSrc"
            val name = fullPath.substring(fullPath.lastIndexOf('/') + 1)
            val entry = IterationEntry(name = name, path = fullPath, module = moduleName, isDir = false)
            evaluator.evaluateIteratorEntry(
                entry, "",
                MatchConfig()
            )?.filePath
        }.sorted() // deterministic order for assertions
    }

    @Test
    fun excludeListTest() {
        val filePaths = listOf("src/main.kt", "src/asd/main.kt", "src/asd/asd.kt", "src/not/asd.kt", "src/nope")
        val results = evaluate(filePaths, setOf("asd", "nope"))
        Assertions.assertEquals(listOf("/main.kt"), results)
    }

    @Test
    fun excludeListTestNoMatches() {
        val filePaths = listOf("src/main.kt", "src/not.kt", "src/dsa/not.kt")
        val results = evaluate(filePaths, setOf("asd"))
        Assertions.assertEquals(setOf("/main.kt", "/not.kt", "/dsa/not.kt"), results.toSet())
    }

    @Test
    fun excludeListTestEmptyList() {
        val filePaths = listOf("src/main.kt", "src/not.kt", "src/dsa/not.kt")
        val results = evaluate(filePaths, emptySet())
        Assertions.assertEquals(setOf("/main.kt", "/not.kt", "/dsa/not.kt"), results.toSet())
    }

    @Test
    fun excludeListTestStartsWith() {
        val filePaths = listOf("src/main.kt", "src/asd/main.kt", "src/asd/asd.kt", "src/not/asd.kt")
        val results = evaluate(filePaths, setOf("/asd*"))
        Assertions.assertEquals(setOf("/main.kt", "/not/asd.kt"), results.toSet())
    }

    @Test
    fun excludeListTestEndsWith() {
        val filePaths = listOf("src/main.log", "src/asd/main.log", "src/asd/asd.kt", "src/not/asd.kt", "src/nope")
        val results = evaluate(filePaths, setOf("*.log"))
        Assertions.assertEquals(setOf("/asd/asd.kt", "/not/asd.kt", "/nope"), results.toSet())
    }

    @Test
    fun testIgnoreEmptyList() {
        val filePaths = listOf("src/dir/file.txt", "src/main.kt", "src/other.kt")
        val results = evaluate(filePaths, emptySet())
        Assertions.assertEquals(setOf("/dir/file.txt", "/main.kt", "/other.kt"), results.toSet())
    }
}