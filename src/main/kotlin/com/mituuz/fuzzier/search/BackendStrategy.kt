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

sealed interface BackendStrategy {
    val name: String

    fun buildCommand(grepConfig: GrepConfig, searchString: String): List<String>

    object Ripgrep : BackendStrategy {
        override val name = "ripgrep"

        override fun buildCommand(grepConfig: GrepConfig, searchString: String): List<String> {
            val commands = mutableListOf("rg")

            if (grepConfig.caseMode == CaseMode.INSENSITIVE) {
                commands.add("--smart-case")
                commands.add("-F")
            }

            commands.addAll(
                mutableListOf(
                    "--no-heading",
                    "--color=never",
                    "-n",
                    "--with-filename",
                    "--column"
                )
            )
            grepConfig.fileGlob.removePrefix(".").takeIf { it.isNotEmpty() }?.let { ext ->
                val glob = "*.${ext}"
                commands.addAll(listOf("-g", glob))
            }
            commands.add(searchString)
            commands.addAll(grepConfig.targets)
            return commands
        }
    }

    object Findstr : BackendStrategy {
        override val name = "findstr"

        override fun buildCommand(grepConfig: GrepConfig, searchString: String): List<String> {
            val commands = mutableListOf("findstr")

            if (grepConfig.caseMode == CaseMode.INSENSITIVE) {
                commands.add("/I")
            }

            commands.addAll(
                mutableListOf(
                    "/p",
                    "/s",
                    "/n",
                    searchString
                )
            )
            commands.addAll(grepConfig.targets)
            return commands
        }
    }

    object Grep : BackendStrategy {
        override val name = "com/mituuz/fuzzier/grep"

        override fun buildCommand(grepConfig: GrepConfig, searchString: String): List<String> {
            val commands = mutableListOf("com/mituuz/fuzzier/grep")

            if (grepConfig.caseMode == CaseMode.INSENSITIVE) {
                commands.add("-i")
            }

            commands.addAll(
                mutableListOf(
                    "--color=none",
                    "-r",
                    "-n",
                    searchString
                )
            )
            commands.addAll(grepConfig.targets)
            return commands
        }
    }
}

