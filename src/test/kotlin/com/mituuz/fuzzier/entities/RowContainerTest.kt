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
        val rc = RowContainer.rowContainerFromString(input, "/base/", true, false)

        assertEquals("/src/main/kotlin/com/mituuz/fuzzier/components/TestBenchComponent.kt", rc.filePath)
        assertEquals("/base/", rc.basePath)
        assertEquals("TestBenchComponent.kt", rc.filename)
        assertEquals(205, rc.rowNumber)
        assertEquals(33, rc.columnNumber)
        assertEquals("            moduleFileIndex.iterateContent(contentIterator)", rc.trimmedRow)
    }

    @Test
    fun fromGrepString() {
        val input = "./src/main/kotlin/com/mituuz/fuzzier/components/TestBenchComponent.kt:205:            moduleFileIndex.iterateContent(contentIterator)"
        val rc = RowContainer.rowContainerFromString(input, "/base/", false, false)

        assertEquals("/src/main/kotlin/com/mituuz/fuzzier/components/TestBenchComponent.kt", rc.filePath)
        assertEquals("/base/", rc.basePath)
        assertEquals("TestBenchComponent.kt", rc.filename)
        assertEquals(205, rc.rowNumber)
        assertEquals(0, rc.columnNumber)
        assertEquals("            moduleFileIndex.iterateContent(contentIterator)", rc.trimmedRow)
    }

    @Test
    fun fromFindstrString() {
        val input = "src\\main\\kotlin\\com\\mituuz\\fuzzier\\components\\TestBenchComponent.kt:205:            moduleFileIndex.iterateContent(contentIterator)"
        kkval rc = RowContainer.rowContainerFromString(input, "/base/", false, true)

        assertEquals("/src\\main\\kotlin\\com\\mituuz\\fuzzier\\components\\TestBenchComponent.kt", rc.filePath)
        assertEquals("/base/", rc.basePath)
        assertEquals("TestBenchComponent.kt", rc.filename)
        assertEquals(205, rc.rowNumber)
        assertEquals(0, rc.columnNumber)
        assertEquals("            moduleFileIndex.iterateContent(contentIterator)", rc.trimmedRow)
    }
}
