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

package com.mituuz.fuzzier.search

import com.intellij.openapi.application.ApplicationManager
import com.intellij.testFramework.TestApplicationManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.mituuz.fuzzier.grep.backend.BackendResolver
import com.mituuz.fuzzier.grep.backend.FuzzierGrep
import com.mituuz.fuzzier.grep.backend.Ripgrep
import com.mituuz.fuzzier.runner.CommandRunner
import com.mituuz.fuzzier.settings.FuzzierGlobalSettingsService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class BackendResolverTest {
    @Suppress("unused")
    private val testApplicationManager: TestApplicationManager = TestApplicationManager.getInstance()
    private lateinit var commandRunner: CommandRunner
    private val projectBasePath = "/test/project"

    @BeforeEach
    fun setUp() {
        commandRunner = mockk(relaxed = true)
    }

    @Test
    fun `resolveBackend returns FuzzierGrep when setting is FUZZIER`() = runBlocking {
        val settings = ApplicationManager.getApplication().getService(FuzzierGlobalSettingsService::class.java)
        settings.state.grepBackend = FuzzierGlobalSettingsService.GrepBackend.FUZZIER

        val resolver = BackendResolver(isWindows = false)
        val result = resolver.resolveBackend(commandRunner, projectBasePath)

        assertTrue(result.isSuccess)
        assertEquals(FuzzierGrep, result.getOrNull())
    }

    @Test
    fun `resolveBackend returns Ripgrep when setting is DYNAMIC and rg is available`() = runBlocking {
        val settings = ApplicationManager.getApplication().getService(FuzzierGlobalSettingsService::class.java)
        settings.state.grepBackend = FuzzierGlobalSettingsService.GrepBackend.DYNAMIC

        val resolver = BackendResolver(isWindows = false)
        coEvery { commandRunner.runCommandForOutput(listOf("which", "rg"), projectBasePath) } returns "/usr/bin/rg"

        val result = resolver.resolveBackend(commandRunner, projectBasePath)

        assertTrue(result.isSuccess)
        assertEquals(Ripgrep, result.getOrNull())
        coVerify { commandRunner.runCommandForOutput(listOf("which", "rg"), projectBasePath) }
    }

    @Test
    fun `resolveBackend returns FuzzierGrep when setting is DYNAMIC and rg is not available`() = runBlocking {
        val settings = ApplicationManager.getApplication().getService(FuzzierGlobalSettingsService::class.java)
        settings.state.grepBackend = FuzzierGlobalSettingsService.GrepBackend.DYNAMIC

        val resolver = BackendResolver(isWindows = false)
        coEvery { commandRunner.runCommandForOutput(listOf("which", "rg"), projectBasePath) } returns ""

        val result = resolver.resolveBackend(commandRunner, projectBasePath)

        assertTrue(result.isSuccess)
        assertEquals(FuzzierGrep, result.getOrNull())
        coVerify { commandRunner.runCommandForOutput(listOf("which", "rg"), projectBasePath) }
    }
}