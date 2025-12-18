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
package com.mituuz.fuzzier

import com.intellij.testFramework.TestApplicationManager
import com.mituuz.fuzzier.search.Fuzzier
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class FuzzierTest {
    @Suppress("unused")
    private var testApplicationManager: TestApplicationManager = TestApplicationManager.getInstance()

    @Test
    fun `actionPerformed does nothing when project is null`() {
        class TestFuzzier : Fuzzier() {
            var ran = false
            override fun runAction(
                project: com.intellij.openapi.project.Project,
                actionEvent: com.intellij.openapi.actionSystem.AnActionEvent
            ) {
                ran = true
            }
        }

        val fuzzier = TestFuzzier()
        val event = mockk<com.intellij.openapi.actionSystem.AnActionEvent>(relaxed = true)
        every { event.project } returns null

        fuzzier.actionPerformed(event)

        assertFalse(fuzzier.ran, "runAction should not be called when project is null")
    }
}
