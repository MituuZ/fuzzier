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
package com.mituuz.fuzzier.actions

import com.intellij.openapi.components.service
import com.intellij.testFramework.TestApplicationManager
import com.mituuz.fuzzier.settings.FuzzierGlobalSettingsService
import com.mituuz.fuzzier.ui.popup.AutoSizePopupProvider
import com.mituuz.fuzzier.ui.popup.DefaultPopupProvider
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

private class TestFuzzyAction : FuzzyAction() {
    override fun runAction(
        project: com.intellij.openapi.project.Project,
        actionEvent: com.intellij.openapi.actionSystem.AnActionEvent
    ) { /* no-op */
    }

    override fun updateListContents(project: com.intellij.openapi.project.Project, searchString: String) { /* no-op */
    }

    fun peekProvider() = getPopupProvider()
}

class FuzzyActionProviderSelectionTest {
    @Suppress("unused")
    private val app = TestApplicationManager.getInstance()
    private val state = service<FuzzierGlobalSettingsService>().state

    private lateinit var action: TestFuzzyAction

    @BeforeEach
    fun setUp() {
        action = TestFuzzyAction()
    }

    @Test
    fun returnsAutoSizeProvider_whenAutoSizeSelected() {
        state.popupSizing = FuzzierGlobalSettingsService.PopupSizing.AUTO_SIZE
        val provider = action.peekProvider()
        assertTrue(provider is AutoSizePopupProvider)
    }

    @Test
    fun returnsDefaultProvider_whenVanillaSelected() {
        state.popupSizing = FuzzierGlobalSettingsService.PopupSizing.VANILLA
        val provider = action.peekProvider()
        assertTrue(provider is DefaultPopupProvider)
    }
}
