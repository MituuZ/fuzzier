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

import com.mituuz.fuzzier.runner.CommandRunner
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class BackendResolverTest {
    private lateinit var commandRunner: CommandRunner
    private val projectBasePath = "/test/project"

    @BeforeEach
    fun setUp() {
        commandRunner = mockk()
    }

    @Test
    fun `resolveBackend returns Ripgrep when rg is available on Windows`() = runBlocking {
        val resolver = BackendResolver(isWindows = true)
        coEvery { commandRunner.runCommandForOutput(listOf("where", "rg"), projectBasePath) } returns "/usr/bin/rg"

        val result = resolver.resolveBackend(commandRunner, projectBasePath)

        assertTrue(result.isSuccess)
        assertEquals(BackendStrategy.Ripgrep, result.getOrNull())
        coVerify { commandRunner.runCommandForOutput(listOf("where", "rg"), projectBasePath) }
    }

    @Test
    fun `resolveBackend returns Ripgrep when rg is available on non-Windows`() = runBlocking {
        val resolver = BackendResolver(isWindows = false)
        coEvery { commandRunner.runCommandForOutput(listOf("which", "rg"), projectBasePath) } returns "/usr/bin/rg"

        val result = resolver.resolveBackend(commandRunner, projectBasePath)

        assertTrue(result.isSuccess)
        assertEquals(BackendStrategy.Ripgrep, result.getOrNull())
        coVerify { commandRunner.runCommandForOutput(listOf("which", "rg"), projectBasePath) }
    }

    @Test
    fun `resolveBackend returns Findstr when rg is not available on Windows`() = runBlocking {
        val resolver = BackendResolver(isWindows = true)
        coEvery { commandRunner.runCommandForOutput(listOf("where", "rg"), projectBasePath) } returns null
        coEvery {
            commandRunner.runCommandForOutput(
                listOf("where", "findstr"), projectBasePath
            )
        } returns "C:\\Windows\\System32\\findstr.exe"

        val result = resolver.resolveBackend(commandRunner, projectBasePath)

        assertTrue(result.isSuccess)
        assertEquals(BackendStrategy.Findstr, result.getOrNull())
        coVerify { commandRunner.runCommandForOutput(listOf("where", "rg"), projectBasePath) }
        coVerify { commandRunner.runCommandForOutput(listOf("where", "findstr"), projectBasePath) }
    }

    @Test
    fun `resolveBackend returns Grep when rg is not available on non-Windows`() = runBlocking {
        val resolver = BackendResolver(isWindows = false)
        coEvery { commandRunner.runCommandForOutput(listOf("which", "rg"), projectBasePath) } returns ""
        coEvery { commandRunner.runCommandForOutput(listOf("which", "grep"), projectBasePath) } returns "/usr/bin/grep"

        val result = resolver.resolveBackend(commandRunner, projectBasePath)

        assertTrue(result.isSuccess)
        assertEquals(BackendStrategy.Grep, result.getOrNull())
        coVerify { commandRunner.runCommandForOutput(listOf("which", "rg"), projectBasePath) }
        coVerify { commandRunner.runCommandForOutput(listOf("which", "grep"), projectBasePath) }
    }

    @Test
    fun `resolveBackend returns failure when no backend is available on Windows`() = runBlocking {
        val resolver = BackendResolver(isWindows = true)
        coEvery {
            commandRunner.runCommandForOutput(
                listOf("where", "rg"), projectBasePath
            )
        } returns "Could not find files"
        coEvery { commandRunner.runCommandForOutput(listOf("where", "findstr"), projectBasePath) } returns null

        val result = resolver.resolveBackend(commandRunner, projectBasePath)

        assertTrue(result.isFailure)
        assertEquals("No suitable grep command found", result.exceptionOrNull()?.message)
    }

    @Test
    fun `resolveBackend returns failure when no backend is available on non-Windows`() = runBlocking {
        val resolver = BackendResolver(isWindows = false)
        coEvery { commandRunner.runCommandForOutput(listOf("which", "rg"), projectBasePath) } returns "   "
        coEvery { commandRunner.runCommandForOutput(listOf("which", "grep"), projectBasePath) } returns ""

        val result = resolver.resolveBackend(commandRunner, projectBasePath)

        assertTrue(result.isFailure)
        assertEquals("No suitable grep command found", result.exceptionOrNull()?.message)
    }

    @Test
    fun `resolveBackend prioritizes Ripgrep over Findstr on Windows`() = runBlocking {
        val resolver = BackendResolver(isWindows = true)
        coEvery { commandRunner.runCommandForOutput(listOf("where", "rg"), projectBasePath) } returns "/usr/bin/rg"
        coEvery {
            commandRunner.runCommandForOutput(
                listOf("where", "findstr"), projectBasePath
            )
        } returns "C:\\Windows\\System32\\findstr.exe"

        val result = resolver.resolveBackend(commandRunner, projectBasePath)

        assertTrue(result.isSuccess)
        assertEquals(BackendStrategy.Ripgrep, result.getOrNull())
        coVerify { commandRunner.runCommandForOutput(listOf("where", "rg"), projectBasePath) }
        coVerify(exactly = 0) { commandRunner.runCommandForOutput(listOf("where", "findstr"), projectBasePath) }
    }
}