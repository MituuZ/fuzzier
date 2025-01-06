package com.mituuz.fuzzier.entities

import com.mituuz.fuzzier.settings.FuzzierSettingsService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class RowContainerTest {
    @Test
    fun displayString() {
        val state = FuzzierSettingsService.State()
        val container = RowContainer("", "", "filename", 0)

        assertEquals("filename:0", container.getDisplayString(state))
    }
}