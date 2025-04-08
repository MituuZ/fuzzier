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
package com.mituuz.fuzzier.entities

import com.intellij.testFramework.TestApplicationManager
import com.mituuz.fuzzier.entities.FuzzyMatchContainer.FuzzyScore
import com.mituuz.fuzzier.settings.FuzzierConfiguration.END_STYLE_TAG
import com.mituuz.fuzzier.settings.FuzzierConfiguration.startStyleTag
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import javax.swing.DefaultListModel

class FuzzyMatchContainerTest {
    @Suppress("unused")
    private val testManager = TestApplicationManager.getInstance()

    @Test
    fun `Test highlight indexing simple case`() {
        val score = FuzzyScore()
        score.highlightCharacters.add(0)
        score.highlightCharacters.add(4)
        val container = FuzzyMatchContainer(score, "", "Hello", "")
        val res = container.highlight(container.filename)
        assertEquals("${startStyleTag}H${END_STYLE_TAG}ell${startStyleTag}o$END_STYLE_TAG", res)
    }

    @Test
    fun `Test highlight indexing sequential indexes`() {
        val score = FuzzyScore()
        score.highlightCharacters.add(0)
        score.highlightCharacters.add(1)
        val container = FuzzyMatchContainer(score, "", "Hello", "")
        val res = container.highlight(container.filename)
        assertEquals("${startStyleTag}He${END_STYLE_TAG}llo", res)
    }

    @Test
    fun `Test highlight indexing complex case`() {
        val score = FuzzyScore()
        score.highlightCharacters.add(0)  // f
        score.highlightCharacters.add(1)  // u
        score.highlightCharacters.add(2)  // z
        score.highlightCharacters.add(3)  // z
        score.highlightCharacters.add(15) // i
        score.highlightCharacters.add(17) // e
        score.highlightCharacters.add(18) // r

        val container = FuzzyMatchContainer(score, "", "FuzzyMatchContainerTest.kt", "")
        val res = container.highlight(container.filename)
        val sb = StringBuilder()

        sb.append(startStyleTag, "Fuzz", END_STYLE_TAG)
        sb.append("yMatchConta")
        sb.append(startStyleTag, "i", END_STYLE_TAG)
        sb.append("n")
        sb.append(startStyleTag, "er", END_STYLE_TAG)
        sb.append("Test.kt")
        var i = 0
        while (i < res.length) {
            assertEquals(sb.toString()[i], res[i])
            i++
        }
    }

    @Test
    fun `Test serialization`() {
        val score = FuzzyScore()
        val container = FuzzyMatchContainer(score, "", "FuzzyMatchContainerTest.kt", "")
        val serializableContainer = FuzzyMatchContainer.SerializedMatchContainer.fromFuzzyMatchContainer(container)
        val byteArrayOutputStream = ByteArrayOutputStream()
        ObjectOutputStream(byteArrayOutputStream).use { it.writeObject(serializableContainer) }

        val byteArrayInputStream = ByteArrayInputStream(byteArrayOutputStream.toByteArray())
        val deserialized = ObjectInputStream(byteArrayInputStream).use { it.readObject() as FuzzyMatchContainer.SerializedMatchContainer }
        val fmc = deserialized.toFuzzyMatchContainer()
        assertEquals("", fmc.filePath)
        assertEquals("FuzzyMatchContainerTest.kt", fmc.filename)
    }

    @Test
    fun `Test default list serialization`() {
        val list = DefaultListModel<FuzzyMatchContainer>()
        val score = FuzzyScore()
        val container = FuzzyMatchContainer(score, "", "FuzzyMatchContainerTest.kt", "")
        list.addElement(container)

        val converter = FuzzyMatchContainer.SerializedMatchContainerConverter()
        val stringRep = converter.toString(FuzzyMatchContainer.SerializedMatchContainer.fromListModel(list))

        val deserialized: DefaultListModel<FuzzyMatchContainer.SerializedMatchContainer> = converter.fromString(stringRep)
        assertEquals(1, deserialized.size)
        assertEquals("", deserialized.get(0).filePath)
        assertEquals("FuzzyMatchContainerTest.kt", deserialized.get(0).filename)
    }

    @Test
    fun `Deserialization fails`() {
        val converter = FuzzyMatchContainer.SerializedMatchContainerConverter()
        val deserialized: DefaultListModel<FuzzyMatchContainer.SerializedMatchContainer> = converter.fromString("This should not work")
        assertEquals(0, deserialized.size)
    }
}