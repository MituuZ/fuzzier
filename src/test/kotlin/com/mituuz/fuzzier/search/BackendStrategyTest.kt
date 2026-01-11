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

import com.mituuz.fuzzier.entities.CaseMode
import com.mituuz.fuzzier.entities.GrepConfig
import com.mituuz.fuzzier.grep.backend.BackendStrategy
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class BackendStrategyTest {

    @Nested
    inner class RipgrepTest {
        @Test
        fun `buildCommand should include basic ripgrep flags`() {
            val config = GrepConfig(
                targets = null,
                caseMode = CaseMode.SENSITIVE,
            )

            val result = com.mituuz.fuzzier.grep.backend.Ripgrep.buildCommand(config, "test", null)

            assertEquals(
                listOf(
                    "rg",
                    "--no-heading",
                    "--color=never",
                    "-n",
                    "--with-filename",
                    "--column",
                    "test",
                    "."
                ),
                result
            )
        }

        @Test
        fun `buildCommand should include file extension glob flag without dot`() {
            val config = GrepConfig(
                targets = null,
                caseMode = CaseMode.SENSITIVE,
            )

            val result = com.mituuz.fuzzier.grep.backend.Ripgrep.buildCommand(config, "test", "java")

            assertEquals(
                listOf(
                    "rg",
                    "--no-heading",
                    "--color=never",
                    "-n",
                    "--with-filename",
                    "--column",
                    "-g",
                    "*.java",
                    "test",
                    "."
                ),
                result
            )
        }

        @Test
        fun `buildCommand should include file extension glob flag with dot`() {
            val config = GrepConfig(
                targets = null,
                caseMode = CaseMode.SENSITIVE,
            )

            val result = com.mituuz.fuzzier.grep.backend.Ripgrep.buildCommand(config, "test", ".java")

            assertEquals(
                listOf(
                    "rg",
                    "--no-heading",
                    "--color=never",
                    "-n",
                    "--with-filename",
                    "--column",
                    "-g",
                    "*.java",
                    "test",
                    "."
                ),
                result
            )
        }

        @Test
        fun `buildCommand should add smart-case and -F for insensitive mode`() {
            val config = GrepConfig(
                targets = null,
                caseMode = CaseMode.INSENSITIVE,
            )

            val result = com.mituuz.fuzzier.grep.backend.Ripgrep.buildCommand(config, "test", null)

            assertTrue(result.contains("--smart-case"))
            assertTrue(result.contains("-F"))
        }

        @Test
        fun `buildCommand should not add smart-case for sensitive mode`() {
            val config = GrepConfig(
                targets = null,
                caseMode = CaseMode.SENSITIVE,
            )

            val result = com.mituuz.fuzzier.grep.backend.Ripgrep.buildCommand(config, "test", null)

            assertTrue(!result.contains("--smart-case"))
            assertTrue(!result.contains("-F"))
        }

        @Test
        fun `name should return ripgrep`() {
            assertEquals("ripgrep", com.mituuz.fuzzier.grep.backend.Ripgrep.name)
        }
    }
}