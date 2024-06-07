package com.mituuz.fuzzier.entities

import com.mituuz.fuzzier.entities.FuzzyMatchContainer.FuzzyScore
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class FuzzyMatchContainerTest {
    @Test
    fun `Test highlight indexing`() {
        val s = FuzzyScore()
        s.highlightCharacters.add(0)
        s.highlightCharacters.add(4)
        val f = FuzzyMatchContainer(s, "", "Hello")
        // assertEquals("<mark>H</mark>ell<mark>o</mark>", f.highlight(f.filename))
        assertEquals("<font style='background-color: yellow;'>H</font>ell<font style='background-color: yellow;'>o</font>", f.highlight(f.filename))
    }
}