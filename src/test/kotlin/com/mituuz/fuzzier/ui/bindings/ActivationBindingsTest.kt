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

package com.mituuz.fuzzier.ui.bindings

import com.intellij.testFramework.TestApplicationManager
import com.mituuz.fuzzier.components.FuzzyComponent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.awt.event.MouseEvent
import javax.swing.JComponent
import javax.swing.KeyStroke

class ActivationBindingsTest {
    @Suppress("unused")
    private var testApplicationManager: TestApplicationManager = TestApplicationManager.getInstance()

    private lateinit var component: FuzzyComponent

    @BeforeEach
    fun setUp() {
        component = FuzzyComponent()
    }

    @Test
    fun `Double-click on list triggers activation`() {
        var activations = 0
        ActivationBindings.install(component, onActivate = { activations++ })

        // Simulate double-click on the file list
        val evt = MouseEvent(
            component.fileList,
            MouseEvent.MOUSE_CLICKED,
            System.currentTimeMillis(),
            0,
            1,
            1,
            2,
            false
        )
        // Dispatch to all listeners
        for (listener in component.fileList.mouseListeners) {
            listener.mouseClicked(evt)
        }

        assertEquals(1, activations)
    }

    @Test
    fun `Enter key binding triggers activation`() {
        var activations = 0
        val actionId = "test.activateSelection"
        ActivationBindings.install(component, onActivate = { activations++ }, actionId = actionId)

        // Verify InputMap contains the mapping for ENTER to our actionId
        val enter = KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ENTER, 0)
        val inputMap = component.searchField.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
        val mapped = inputMap.get(enter)
        assertEquals(actionId, mapped)

        // Retrieve and invoke the action directly from the ActionMap
        val action = component.searchField.actionMap.get(actionId)
        action.actionPerformed(null)

        assertEquals(1, activations)
    }
}