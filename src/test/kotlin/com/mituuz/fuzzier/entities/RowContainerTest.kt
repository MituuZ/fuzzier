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

import com.mituuz.fuzzier.settings.FuzzierGlobalSettingsService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class RowContainerTest {
    @Test
    fun displayString() {
        val state = FuzzierGlobalSettingsService.State()
        val container = RowContainer("", "", "filename", 0, 3, "trimmed row content")

        assertEquals("filename 0:3:trimmed row content", container.getDisplayString(state))
    }

    @Test
    fun fromRGString() {
        val input = "./src/main/kotlin/com/mituuz/fuzzier/components/TestBenchComponent.kt:205:33:            moduleFileIndex.iterateContent(contentIterator)"
        val rc = RowContainer.rowContainerFromString(input, "/base/", true, isWindows = false)

        assertEquals("/src/main/kotlin/com/mituuz/fuzzier/components/TestBenchComponent.kt", rc.filePath)
        assertEquals("/base/", rc.basePath)
        assertEquals("TestBenchComponent.kt", rc.filename)
        assertEquals(204, rc.rowNumber)
        assertEquals(32, rc.columnNumber)
        assertEquals("            moduleFileIndex.iterateContent(contentIterator)", rc.trimmedRow)
    }

    @Test
    fun fromRGString_complexString() {
        val input = "./src/main/kotlin/com/example/Test.kt:42:15:val x = \"Hello:World\"; // Contains: colon:in:string and comment"
        val rc = RowContainer.rowContainerFromString(input, "/base/", true, isWindows = false)

        assertEquals("/src/main/kotlin/com/example/Test.kt", rc.filePath)
        assertEquals("/base/", rc.basePath)
        assertEquals("Test.kt", rc.filename)
        assertEquals(41, rc.rowNumber)
        assertEquals(14, rc.columnNumber)
        assertEquals("val x = \"Hello:World\"; // Contains: colon:in:string and comment", rc.trimmedRow)
    }

    @Test
    fun fromGrepString_complexString() {
        val input = "./src/main/kotlin/com/mituuz/fuzzier/components/TestBenchComponent.kt:205:val message = Map.of(\"key:1\", \"value:2\"); // Multiple: colons: here"
        val rc = RowContainer.rowContainerFromString(input, "/base/", false, isWindows = false)

        assertEquals("/src/main/kotlin/com/mituuz/fuzzier/components/TestBenchComponent.kt", rc.filePath)
        assertEquals("/base/", rc.basePath)
        assertEquals("TestBenchComponent.kt", rc.filename)
        assertEquals(204, rc.rowNumber)
        assertEquals(0, rc.columnNumber)
        assertEquals("val message = Map.of(\"key:1\", \"value:2\"); // Multiple: colons: here", rc.trimmedRow)
    }

    @Test
    fun fromGrepString() {
        val input = "./src/main/kotlin/com/mituuz/fuzzier/components/TestBenchComponent.kt:205:            moduleFileIndex.iterateContent(contentIterator)"
        val rc = RowContainer.rowContainerFromString(input, "/base/", false, isWindows = false)

        assertEquals("/src/main/kotlin/com/mituuz/fuzzier/components/TestBenchComponent.kt", rc.filePath)
        assertEquals("/base/", rc.basePath)
        assertEquals("TestBenchComponent.kt", rc.filename)
        assertEquals(204, rc.rowNumber)
        assertEquals(0, rc.columnNumber)
        assertEquals("            moduleFileIndex.iterateContent(contentIterator)", rc.trimmedRow)
    }

    @Test
    fun fromFindstrString() {
        val input = "src\\main\\kotlin\\com\\mituuz\\fuzzier\\components\\TestBenchComponent.kt:205:            moduleFileIndex.iterateContent(contentIterator)"
        val rc = RowContainer.rowContainerFromString(input, "/base/", false, isWindows = true)

        assertEquals("/src\\main\\kotlin\\com\\mituuz\\fuzzier\\components\\TestBenchComponent.kt", rc.filePath)
        assertEquals("/base/", rc.basePath)
        assertEquals("TestBenchComponent.kt", rc.filename)
        assertEquals(204, rc.rowNumber)
        assertEquals(0, rc.columnNumber)
        assertEquals("            moduleFileIndex.iterateContent(contentIterator)", rc.trimmedRow)
    }
}
