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

// filepath: /home/mituuz/IdeaProjects/fuzzier/src/test/kotlin/com/mituuz/fuzzier/components/FuzzyFinderComponentTest.kt
package com.mituuz.fuzzier.components

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.testFramework.TestApplicationManager
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.runInEdtAndWait
import com.mituuz.fuzzier.TestUtil
import com.mituuz.fuzzier.settings.FuzzierGlobalSettingsService
import com.mituuz.fuzzier.settings.FuzzierGlobalSettingsService.SearchPosition.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger

class FuzzyFinderComponentTest {
    companion object {
        @JvmStatic
        @BeforeAll
        fun initApp() {
            // Ensure IDEA test application initialized
            TestApplicationManager.getInstance()
        }
    }

    private val testUtil = TestUtil()
    private lateinit var fixture: CodeInsightTestFixture
    private val settingsService: FuzzierGlobalSettingsService by lazy { service<FuzzierGlobalSettingsService>() }
    private var originalSearchPosition: FuzzierGlobalSettingsService.SearchPosition? = null
    private var originalWidth: Int = 0
    private var originalHeight: Int = 0

    @BeforeEach
    fun setUp() {
        fixture = testUtil.setUpProject(emptyList())
        val state = settingsService.state
        originalSearchPosition = state.searchPosition
        originalWidth = state.defaultPopupWidth
        originalHeight = state.defaultPopupHeight
    }

    @AfterEach
    fun tearDown() {
        val state = settingsService.state
        if (originalSearchPosition != null) state.searchPosition = originalSearchPosition!!
        state.defaultPopupWidth = originalWidth
        state.defaultPopupHeight = originalHeight
        fixture.tearDown()
    }

    @Test
    fun `Default LEFT orientation puts preview on right`() {
        val state = settingsService.state
        state.searchPosition = LEFT
        runInEdtAndWait {
            val component = FuzzyFinderComponent(fixture.project, showSecondaryField = false)
            // Horizontal split -> orientation=false, preview on right -> second component
            assertFalse(component.splitPane.orientation)
            assertSame(component.previewPane, component.splitPane.secondComponent)
            assertNotNull(component.splitPane.firstComponent)
            assertNotSame(component.previewPane, component.splitPane.firstComponent)
        }
    }

    @Test
    fun `RIGHT orientation puts preview on left`() {
        val state = settingsService.state
        state.searchPosition = RIGHT
        runInEdtAndWait {
            val component = FuzzyFinderComponent(fixture.project, showSecondaryField = false)
            // Horizontal split -> orientation=false, preview on left -> first component
            assertFalse(component.splitPane.orientation)
            assertSame(component.previewPane, component.splitPane.firstComponent)
            assertNotNull(component.splitPane.secondComponent)
            assertNotSame(component.previewPane, component.splitPane.secondComponent)
        }
    }

    @Test
    fun `TOP orientation uses vertical split with preview at bottom`() {
        val state = settingsService.state
        state.searchPosition = TOP
        runInEdtAndWait {
            val component = FuzzyFinderComponent(fixture.project, showSecondaryField = false)
            // Vertical split -> orientation=true, preview at bottom -> second component
            assertTrue(component.splitPane.orientation)
            assertSame(component.previewPane, component.splitPane.secondComponent)
            assertNotSame(component.previewPane, component.splitPane.firstComponent)
        }
    }

    @Test
    fun `BOTTOM orientation uses vertical split with preview at top`() {
        val state = settingsService.state
        state.searchPosition = BOTTOM
        runInEdtAndWait {
            val component = FuzzyFinderComponent(fixture.project, showSecondaryField = false)
            // Vertical split -> orientation=true, preview at top -> first component
            assertTrue(component.splitPane.orientation)
            assertSame(component.previewPane, component.splitPane.firstComponent)
            assertNotSame(component.previewPane, component.splitPane.secondComponent)
        }
    }

    @Test
    fun `Preferred size uses state dimensions`() {
        val state = settingsService.state
        state.searchPosition = LEFT
        state.defaultPopupWidth = 777
        state.defaultPopupHeight = 444
        runInEdtAndWait {
            val component = FuzzyFinderComponent(fixture.project, showSecondaryField = false)
            val pref = component.splitPane.preferredSize
            assertEquals(777, pref.width)
            assertEquals(444, pref.height)
        }
    }

    @Test
    fun `Secondary field text and listener triggered`() {
        val state = settingsService.state
        state.searchPosition = LEFT
        runInEdtAndWait {
            val component = FuzzyFinderComponent(fixture.project, showSecondaryField = true)

            assertEquals("", component.getSecondaryText())
            val changeCount = AtomicInteger(0)
            val disposable = Disposable { }
            component.addSecondaryDocumentListener(object : DocumentListener {
                override fun documentChanged(event: DocumentEvent) {
                    changeCount.incrementAndGet()
                }
            }, disposable)

            val secondary = component.getSecondaryField()
            secondary.text = "hello"
            assertEquals("hello", component.getSecondaryText())
            secondary.text = "world"
            assertEquals("world", component.getSecondaryText())
            assertTrue(changeCount.get() == 2, "Expected 2 changes, got ${changeCount.get()}")
        }
    }
}
