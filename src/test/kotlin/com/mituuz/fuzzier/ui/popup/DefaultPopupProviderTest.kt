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

package com.mituuz.fuzzier.ui.popup

import com.intellij.openapi.ui.popup.LightweightWindow
import com.intellij.openapi.ui.popup.LightweightWindowEvent
import com.intellij.testFramework.TestApplicationManager
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.awt.Dimension
import javax.swing.JLabel
import javax.swing.JPanel

class DefaultPopupProviderTest {
    @Suppress("unused")
    private var testApplicationManager: TestApplicationManager = TestApplicationManager.getInstance()

    private lateinit var provider: DefaultPopupProvider
    private lateinit var fixture: IdeaProjectTestFixture
    private lateinit var codeFixture: CodeInsightTestFixture

    @BeforeEach
    fun setUp() {
        provider = DefaultPopupProvider()
    }

    @Test
    fun `show returns null and exits when no IDE frame is available`() {
        // Set up a lightweight project fixture to get a Project instance
        val factory = IdeaTestFixtureFactory.getFixtureFactory()
        fixture = factory.createLightFixtureBuilder(null, "Test").fixture
        codeFixture = IdeaTestFixtureFactory.getFixtureFactory().createCodeInsightFixture(fixture)
        codeFixture.setUp()
        val project = fixture.project
        val content = JPanel()
        val focus = JLabel("focus")
        var cleared = false
        val cfg = PopupConfig(
            title = "Test",
            dimensionKey = "fuzzier.test",
            preferredSizeProvider = Dimension(800, 600),
            resetWindow = { true },
            clearResetWindowFlag = { cleared = true }
        )

        try {
            val popup = provider.show(project, content, focus, cfg) { /* cleanup */ }
            // Should return null and not throw
            assertNull(popup)
            // Since we exit early due to missing IDE frame, ensure no side-effects were triggered
            assertEquals(false, cleared)
        } finally {
            codeFixture.tearDown()
        }
    }

    @Test
    fun `cleanup function is called when popup is closed`() {
        var cleanupCalled = false
        val listener = provider.createCleanupListener { cleanupCalled = true }

        val lw = object : LightweightWindow {}

        val evt = LightweightWindowEvent(lw, true)
        listener.onClosed(evt)

        assertEquals(true, cleanupCalled)
    }
}