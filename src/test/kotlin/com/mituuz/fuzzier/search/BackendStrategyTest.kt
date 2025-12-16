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
                targets = listOf("/path/to/search"),
                caseMode = CaseMode.SENSITIVE,
                fileGlob = ""
            )

            val result = BackendStrategy.Ripgrep.buildCommand(config, "test")

            assertEquals(
                listOf(
                    "rg",
                    "--no-heading",
                    "--color=never",
                    "-n",
                    "--with-filename",
                    "--column",
                    "test",
                    "/path/to/search"
                ),
                result
            )
        }

        @Test
        fun `buildCommand should add smart-case and -F for insensitive mode`() {
            val config = GrepConfig(
                targets = listOf("/path"),
                caseMode = CaseMode.INSENSITIVE,
                fileGlob = ""
            )

            val result = BackendStrategy.Ripgrep.buildCommand(config, "test")

            assertTrue(result.contains("--smart-case"))
            assertTrue(result.contains("-F"))
        }

        @Test
        fun `buildCommand should not add smart-case for sensitive mode`() {
            val config = GrepConfig(
                targets = listOf("/path"),
                caseMode = CaseMode.SENSITIVE,
                fileGlob = ""
            )

            val result = BackendStrategy.Ripgrep.buildCommand(config, "test")

            assertTrue(!result.contains("--smart-case"))
            assertTrue(!result.contains("-F"))
        }

        @Test
        fun `buildCommand should add file glob when provided`() {
            val config = GrepConfig(
                targets = listOf("/path"),
                caseMode = CaseMode.SENSITIVE,
                fileGlob = ".kt"
            )

            val result = BackendStrategy.Ripgrep.buildCommand(config, "test")

            val globIndex = result.indexOf("-g")
            assertTrue(globIndex >= 0)
            assertEquals("*.kt", result[globIndex + 1])
        }

        @Test
        fun `buildCommand should handle file glob without leading dot`() {
            val config = GrepConfig(
                targets = listOf("/path"),
                caseMode = CaseMode.SENSITIVE,
                fileGlob = "java"
            )

            val result = BackendStrategy.Ripgrep.buildCommand(config, "test")

            val globIndex = result.indexOf("-g")
            assertTrue(globIndex >= 0)
            assertEquals("*.java", result[globIndex + 1])
        }

        @Test
        fun `buildCommand should not add glob for empty fileGlob`() {
            val config = GrepConfig(
                targets = listOf("/path"),
                caseMode = CaseMode.SENSITIVE,
                fileGlob = ""
            )

            val result = BackendStrategy.Ripgrep.buildCommand(config, "test")

            assertTrue(!result.contains("-g"))
        }

        @Test
        fun `name should return ripgrep`() {
            assertEquals("ripgrep", BackendStrategy.Ripgrep.name)
        }
    }

    @Nested
    inner class FindstrTest {
        @Test
        fun `buildCommand should include basic findstr flags`() {
            val config = GrepConfig(
                targets = listOf("C:\\path\\to\\search"),
                caseMode = CaseMode.SENSITIVE,
                fileGlob = ""
            )

            val result = BackendStrategy.Findstr.buildCommand(config, "test")

            assertTrue(result.contains("findstr"))
            assertTrue(result.contains("/p"))
            assertTrue(result.contains("/s"))
            assertTrue(result.contains("/n"))
            assertTrue(result.contains("test"))
            assertTrue(result.contains("C:\\path\\to\\search"))
        }

        @Test
        fun `buildCommand should add case insensitive flag when needed`() {
            val config = GrepConfig(
                targets = listOf("C:\\path"),
                caseMode = CaseMode.INSENSITIVE,
                fileGlob = ""
            )

            val result = BackendStrategy.Findstr.buildCommand(config, "test")

            assertTrue(result.contains("/I"))
        }

        @Test
        fun `buildCommand should not add case insensitive flag for sensitive mode`() {
            val config = GrepConfig(
                targets = listOf("C:\\path"),
                caseMode = CaseMode.SENSITIVE,
                fileGlob = ""
            )

            val result = BackendStrategy.Findstr.buildCommand(config, "test")

            assertTrue(!result.contains("/I"))
        }

        @Test
        fun `name should return findstr`() {
            assertEquals("findstr", BackendStrategy.Findstr.name)
        }
    }

    @Nested
    inner class GrepTest {
        @Test
        fun `buildCommand should include basic grep flags`() {
            val config = GrepConfig(
                targets = listOf("/path/to/search"),
                caseMode = CaseMode.SENSITIVE,
                fileGlob = ""
            )

            val result = BackendStrategy.Grep.buildCommand(config, "test")

            assertTrue(result.contains("com/mituuz/fuzzier/grep"))
            assertTrue(result.contains("--color=none"))
            assertTrue(result.contains("-r"))
            assertTrue(result.contains("-n"))
            assertTrue(result.contains("test"))
            assertTrue(result.contains("/path/to/search"))
        }

        @Test
        fun `buildCommand should add case insensitive flag when needed`() {
            val config = GrepConfig(
                targets = listOf("/path"),
                caseMode = CaseMode.INSENSITIVE,
                fileGlob = ""
            )

            val result = BackendStrategy.Grep.buildCommand(config, "test")

            assertTrue(result.contains("-i"))
        }

        @Test
        fun `buildCommand should not add case insensitive flag for sensitive mode`() {
            val config = GrepConfig(
                targets = listOf("/path"),
                caseMode = CaseMode.SENSITIVE,
                fileGlob = ""
            )

            val result = BackendStrategy.Grep.buildCommand(config, "test")

            assertTrue(!result.contains("-i"))
        }

        @Test
        fun `name should return grep`() {
            assertEquals("com/mituuz/fuzzier/grep", BackendStrategy.Grep.name)
        }
    }
}