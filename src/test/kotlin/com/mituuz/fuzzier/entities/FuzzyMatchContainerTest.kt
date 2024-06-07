package com.mituuz.fuzzier.entities

import com.intellij.testFramework.TestApplicationManager
import com.mituuz.fuzzier.entities.FuzzyMatchContainer.FuzzyScore
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class FuzzyMatchContainerTest {
    @Test
    fun `Test highlight indexing`() {
        val testManager = TestApplicationManager.getInstance()
        val s = FuzzyScore()
        s.highlightCharacters.add(0)
        s.highlightCharacters.add(4)
        val f = FuzzyMatchContainer(s, "", "Hello")
        assertEquals("<font style='background-color: yellow;'>H</font>ell<font style='background-color: yellow;'>o</font>", f.highlight(f.filename))
    }
}