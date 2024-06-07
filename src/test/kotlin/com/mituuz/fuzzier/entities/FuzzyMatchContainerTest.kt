package com.mituuz.fuzzier.entities

import com.intellij.testFramework.TestApplicationManager
import com.intellij.ui.JBColor
import com.mituuz.fuzzier.entities.FuzzyMatchContainer.Companion.colorToHtml
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
        yellow = colorToHtml(JBColor.YELLOW)
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
        s.highlightCharacters.add(0) // f
        s.highlightCharacters.add(1) // u
        s.highlightCharacters.add(2) // z
        s.highlightCharacters.add(3) // z
        s.highlightCharacters.add(15) // i
        s.highlightCharacters.add(17) // e
        s.highlightCharacters.add(18) // r

        val f = FuzzyMatchContainer(s, "", "FuzzyMatchContainerTest.kt")
        yellow = colorToHtml(JBColor.YELLOW)
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