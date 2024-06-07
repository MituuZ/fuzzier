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
package com.mituuz.fuzzier.entities

import com.intellij.testFramework.TestApplicationManager
import com.intellij.ui.JBColor
import com.mituuz.fuzzier.entities.FuzzyMatchContainer.Companion.colorAsHex
import com.mituuz.fuzzier.entities.FuzzyMatchContainer.FuzzyScore
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FuzzyMatchContainerTest {
    @Suppress("unused")
    private val testManager = TestApplicationManager.getInstance()
    private lateinit var yellow: String
    private lateinit var startTag: String
    private var endTag = "</font>"

    @BeforeEach
    fun setUp() {
        yellow = colorAsHex(JBColor.YELLOW)
        startTag = "<font style='background-color: $yellow;'>"
    }

    @Test
    fun `Test highlight indexing simple case`() {
        val s = FuzzyScore()
        s.highlightCharacters.add(0)
        s.highlightCharacters.add(4)
        val f = FuzzyMatchContainer(s, "", "Hello")
        val res = f.highlight(f.filename)
        assertEquals("${startTag}H${endTag}ell${startTag}o$endTag", res)
    }

    @Test
    fun `Test highlight indexing complex case`() {
        val s = FuzzyScore()
        s.highlightCharacters.add(0)  // f
        s.highlightCharacters.add(1)  // u
        s.highlightCharacters.add(2)  // z
        s.highlightCharacters.add(3)  // z
        s.highlightCharacters.add(15) // i
        s.highlightCharacters.add(17) // e
        s.highlightCharacters.add(18) // r

        val f = FuzzyMatchContainer(s, "", "FuzzyMatchContainerTest.kt")
        yellow = colorAsHex(JBColor.YELLOW)
        val res = f.highlight(f.filename)
        val sb = StringBuilder()

        sb.append(startTag, "F", endTag)
        sb.append(startTag, "u", endTag)
        sb.append(startTag, "z", endTag)
        sb.append(startTag, "z", endTag)
        sb.append("yMatchConta")
        sb.append(startTag, "i", endTag)
        sb.append("n")
        sb.append(startTag, "e", endTag)
        sb.append(startTag, "r", endTag)
        sb.append("Test.kt")
        var i = 0
        while (i < res.length) {
            assertEquals(sb.toString()[i], res[i])
            i++
        }
    }
}