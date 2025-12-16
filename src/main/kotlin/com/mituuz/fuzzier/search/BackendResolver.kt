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

class BackendResolver(val isWindows: Boolean) {
    suspend fun resolveBackend(commandRunner: CommandRunner, projectBasePath: String): Result<BackendStrategy> {
        return when {
            isInstalled(commandRunner, "rg", projectBasePath) -> Result.success(BackendStrategy.Ripgrep)
            isWindows && isInstalled(
                commandRunner,
                "findstr",
                projectBasePath
            ) -> Result.success(BackendStrategy.Findstr)

            !isWindows && isInstalled(commandRunner, "grep", projectBasePath) -> Result.success(BackendStrategy.Grep)
            else -> Result.failure(Exception("No suitable grep command found"))
        }
    }

    private suspend fun isInstalled(
        commandRunner: CommandRunner,
        executable: String,
        projectBasePath: String
    ): Boolean {
        val command = if (isWindows) {
            listOf("where", executable)
        } else {
            listOf("which", executable)
        }

        val result = commandRunner.runCommandForOutput(command, projectBasePath)

        return !(result.isNullOrBlank() || result.contains("Could not find files"))
    }
}