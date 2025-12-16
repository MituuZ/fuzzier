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

package com.mituuz.fuzzier.runner

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessListener
import com.intellij.openapi.util.Key
import com.mituuz.fuzzier.entities.FuzzyContainer
import com.mituuz.fuzzier.search.BackendStrategy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.swing.DefaultListModel

class DefaultCommandRunner : CommandRunner {
    companion object {
        const val MAX_OUTPUT_SIZE = 10000
        const val MAX_NUMBER_OR_RESULTS = 1000
    }

    override suspend fun runCommandForOutput(
        commands: List<String>,
        projectBasePath: String
    ): String? {
        return try {
            val commandLine = GeneralCommandLine(commands)
                .withWorkDirectory(projectBasePath)
                .withRedirectErrorStream(true)
            val output = StringBuilder()
            val processHandler = OSProcessHandler(commandLine)

            processHandler.addProcessListener(object : ProcessListener {
                override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
                    if (output.length < MAX_OUTPUT_SIZE) {
                        output.appendLine(event.text.replace("\n", ""))
                    }
                }
            })

            withContext(Dispatchers.IO) {
                processHandler.startNotify()
                processHandler.waitFor(2000)
            }
            output.toString()
        } catch (_: InterruptedException) {
            throw InterruptedException()
        }
    }

    /**
     * Run the command and stream a limited number of results to the list model
     */
    override suspend fun runCommandPopulateListModel(
        commands: List<String>,
        listModel: DefaultListModel<FuzzyContainer>,
        projectBasePath: String,
        backend: BackendStrategy
    ) {
        try {
            val commandLine = GeneralCommandLine(commands)
                .withWorkDirectory(projectBasePath)
                .withRedirectErrorStream(true)

            val processHandler = OSProcessHandler(commandLine)
            var count = 0

            processHandler.addProcessListener(object : ProcessListener {
                override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
                    if (count >= MAX_NUMBER_OR_RESULTS) return

                    event.text.lines().forEach { line ->
                        if (count >= MAX_NUMBER_OR_RESULTS) return@forEach
                        if (line.isNotBlank()) {
                            val rowContainer = backend.parseOutputLine(line, projectBasePath)
                            if (rowContainer != null) {
                                listModel.addElement(rowContainer)
                                count++
                            }
                        }
                    }
                }
            })

            withContext(Dispatchers.IO) {
                processHandler.startNotify()
                processHandler.waitFor(2000)
            }
        } catch (_: InterruptedException) {
            throw InterruptedException()
        }
    }
}