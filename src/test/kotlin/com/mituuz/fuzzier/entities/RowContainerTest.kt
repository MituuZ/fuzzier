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

package com.mituuz.fuzzier.entities

import com.mituuz.fuzzier.entities.RowContainer.Companion.rgRowContainerFromString
import com.mituuz.fuzzier.entities.RowContainer.Companion.rowContainerFromString
import com.mituuz.fuzzier.settings.FuzzierGlobalSettingsService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS

class RowContainerTest {
    @Test
    fun displayString() {
        val state = FuzzierGlobalSettingsService.State()
        val container = RowContainer("", "", "filename", 0, "trimmed row content", 3)

        assertEquals("filename:0:3: trimmed row content", container.getDisplayString(state))
    }

    @Test
    @EnabledOnOs(OS.LINUX, OS.MAC)
    fun fromRGString() {
        val input =
            "./src/main/kotlin/com/mituuz/fuzzier/components/TestBenchComponent.kt:205:33:            moduleFileIndex.iterateContent(contentIterator)"
        val basePath = "/home/user/IdeaProjects/fuzzier"
        val rc = rgRowContainerFromString(input, basePath)

        assertNotNull(rc, "Could not create row container from $input")
        assertEquals("/src/main/kotlin/com/mituuz/fuzzier/components/TestBenchComponent.kt", rc.filePath)
        assertEquals(basePath, rc.basePath)
        assertEquals("TestBenchComponent.kt", rc.filename)
        assertEquals(204, rc.rowNumber)
        assertEquals(32, rc.columnNumber)
        assertEquals("moduleFileIndex.iterateContent(contentIterator)", rc.trimmedRow)
    }

    @Test
    @EnabledOnOs(OS.LINUX, OS.MAC)
    fun fromGrepString() {
        val input =
            "./src/main/kotlin/com/mituuz/fuzzier/components/TestBenchComponent.kt:205:            moduleFileIndex.iterateContent(contentIterator)"
        val basePath = "/home/user/IdeaProjects/fuzzier"
        val rc = rowContainerFromString(input, basePath)

        assertNotNull(rc, "Could not create row container from $input")
        assertEquals("/src/main/kotlin/com/mituuz/fuzzier/components/TestBenchComponent.kt", rc.filePath)
        assertEquals(basePath, rc.basePath)
        assertEquals("TestBenchComponent.kt", rc.filename)
        assertEquals(204, rc.rowNumber)
        assertEquals(0, rc.columnNumber)
        assertEquals("moduleFileIndex.iterateContent(contentIterator)", rc.trimmedRow)
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    fun fromRGString_windows() {
        val input =
            ".\\src\\main\\kotlin\\com\\mituuz\\fuzzier\\components\\TestBenchComponent.kt:205:33:            moduleFileIndex.iterateContent(contentIterator)"
        val basePath = "C:\\Users\\user\\IdeaProjects\\fuzzier"
        val rc = rgRowContainerFromString(input, basePath)

        assertNotNull(rc, "Could not create row container from $input")
        assertEquals("\\src\\main\\kotlin\\com\\mituuz\\fuzzier\\components\\TestBenchComponent.kt", rc.filePath)
        assertEquals(basePath, rc.basePath)
        assertEquals("TestBenchComponent.kt", rc.filename)
        assertEquals(204, rc.rowNumber)
        assertEquals(32, rc.columnNumber)
        assertEquals("moduleFileIndex.iterateContent(contentIterator)", rc.trimmedRow)
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    fun fromFindstrString_windows() {
        val input =
            ".\\src\\main\\kotlin\\com\\mituuz\\fuzzier\\components\\TestBenchComponent.kt:205:            moduleFileIndex.iterateContent(contentIterator)"
        val basePath = "C:\\Users\\user\\IdeaProjects\\fuzzier"
        val rc = rowContainerFromString(input, basePath)

        assertNotNull(rc, "Could not create row container from $input")
        assertEquals("\\src\\main\\kotlin\\com\\mituuz\\fuzzier\\components\\TestBenchComponent.kt", rc.filePath)
        assertEquals(basePath, rc.basePath)
        assertEquals("TestBenchComponent.kt", rc.filename)
        assertEquals(204, rc.rowNumber)
        assertEquals(0, rc.columnNumber)
        assertEquals("moduleFileIndex.iterateContent(contentIterator)", rc.trimmedRow)
    }

    @Test
    fun fromGrepStringInvalidPattern() {
        val input = "invalid input without proper format"
        val basePath = "/home/user/IdeaProjects/fuzzier"
        val rc = rowContainerFromString(input, basePath)

        assertNull(rc, "Expected null for input that doesn't match COMMON_PATTERN")
    }

    @Test
    fun fromGrepStringInsufficientParts() {
        val input = "file.kt:205"
        val basePath = "/home/user/IdeaProjects/fuzzier"
        val rc = rowContainerFromString(input, basePath)

        assertNull(rc, "Expected null for input with insufficient parts")
    }

    @Test
    fun fromRGStringInvalidPattern() {
        val input = "./file.kt:205: content without column number"
        val basePath = "/home/user/IdeaProjects/fuzzier"
        val rc = rgRowContainerFromString(input, basePath)

        assertNull(rc, "Expected null for input that doesn't match RG_PATTERN")
    }

    @Test
    fun fromRGStringInsufficientParts() {
        val input = "file.kt:205:33"
        val basePath = "/home/user/IdeaProjects/fuzzier"
        val rc = rgRowContainerFromString(input, basePath)

        assertNull(rc, "Expected null for input with insufficient parts")
    }
}
