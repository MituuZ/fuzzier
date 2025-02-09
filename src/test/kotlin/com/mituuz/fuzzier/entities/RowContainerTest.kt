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
}