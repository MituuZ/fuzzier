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

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessListener
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.mituuz.fuzzier.FuzzyGrep.Companion.MAX_NUMBER_OR_RESULTS
import com.mituuz.fuzzier.FuzzyGrep.Companion.MAX_OUTPUT_SIZE
import com.mituuz.fuzzier.actions.FuzzyAction
import com.mituuz.fuzzier.components.FuzzyFinderComponent
import com.mituuz.fuzzier.entities.FuzzyContainer
import com.mituuz.fuzzier.entities.RowContainer
import com.mituuz.fuzzier.ui.bindings.ActivationBindings
import com.mituuz.fuzzier.ui.popup.DefaultPopupProvider
import com.mituuz.fuzzier.ui.popup.PopupConfig
import kotlinx.coroutines.*
import org.apache.commons.lang3.StringUtils
import javax.swing.DefaultListModel
import javax.swing.ListModel

open class FuzzyGrep : FuzzyAction() {
    companion object {
        const val FUZZIER_NOTIFICATION_GROUP: String = "Fuzzier Notification Group"

        /**
         * Limit command output size, this is only used to check installations
         */
        const val MAX_OUTPUT_SIZE = 10000
        const val MAX_NUMBER_OR_RESULTS = 1000
    }

    var useRg = true
    val isWindows = System.getProperty("os.name").lowercase().contains("win")
    private var currentLaunchJob: Job? = null
    private val popupProvider = DefaultPopupProvider()
    protected open lateinit var popupTitle: String

    override fun runAction(
        project: Project,
        actionEvent: AnActionEvent
    ) {
        currentLaunchJob?.cancel()

        val projectBasePath = project.basePath.toString()
        currentLaunchJob = actionScope?.launch(Dispatchers.EDT) {
            val currentJob = currentLaunchJob

            if (!isInstalled("rg", projectBasePath)) {
                showNotification(
                    "No `rg` command found",
                    """
                    No ripgrep found<br>
                    Fallback to `grep` or `findstr`<br>
                    This notification can be disabled
                """.trimIndent(),
                    project,
                    NotificationType.WARNING
                )

                if (isWindows) {
                    if (!isInstalled("findstr", projectBasePath)) {
                        showNotification(
                            "No `findstr` command found",
                            "Fuzzy Grep failed: no `findstr` found",
                            project
                        )
                        return@launch
                    }
                    popupTitle = "Fuzzy Grep (findstr)"
                } else {
                    if (!isInstalled("grep", projectBasePath)) {
                        showNotification(
                            "No `grep` command found",
                            "Fuzzy Grep failed: no `grep` found",
                            project
                        )
                        return@launch
                    }
                    popupTitle = "Fuzzy Grep (grep)"
                }
                useRg = false
            } else {
                popupTitle = "Fuzzy Grep (ripgrep)"
                useRg = true
            }

            if (currentJob?.isCancelled == true) return@launch

            yield()
            defaultDoc = EditorFactory.getInstance().createDocument("")
            component = FuzzyFinderComponent(project, showSecondaryField = useRg)
            createListeners(project)
            popup = popupProvider.show(
                project = project,
                content = component,
                focus = component.searchField,
                config = PopupConfig(
                    title = popupTitle,
                    preferredSizeProvider = component.preferredSize,
                    dimensionKey = "FuzzyGrepPopup",
                    resetWindow = { globalState.resetWindow },
                    clearResetWindowFlag = { globalState.resetWindow = false }
                ),
                cleanupFunction = { cleanupPopup() },
            )

            createSharedListeners(project)

            (component as FuzzyFinderComponent).splitPane.dividerLocation =
                globalState.splitPosition
        }
    }

    private fun showNotification(
        title: String,
        content: String,
        project: Project,
        type: NotificationType = NotificationType.ERROR
    ) {
        val grepNotification = Notification(
            FUZZIER_NOTIFICATION_GROUP,
            title,
            content,
            type
        )
        Notifications.Bus.notify(grepNotification, project)
    }

    override fun onPopupClosed() {
        globalState.splitPosition =
            (component as FuzzyFinderComponent).splitPane.dividerLocation

        currentLaunchJob?.cancel()
        currentLaunchJob = null
    }

    /**
     * OS-specific to see if a specific executable is found
     * @return true if the command was found (possibly not a general check/solution)
     */
    private suspend fun isInstalled(executable: String, projectBasePath: String): Boolean {
        val command = if (isWindows) {
            listOf("where", executable)
        } else {
            listOf("which", executable)
        }

        val result = runCommand(command, projectBasePath)

        return !(result.isNullOrBlank() || result.contains("Could not find files"))
    }

    override fun updateListContents(project: Project, searchString: String) {
        if (StringUtils.isBlank(searchString)) {
            component.fileList.model = DefaultListModel()
            return
        }

        currentUpdateListContentJob?.cancel()
        currentUpdateListContentJob = actionScope?.launch(Dispatchers.EDT) {
            component.fileList.setPaintBusy(true)
            try {
                val results = withContext(Dispatchers.IO) {
                    findInFiles(searchString, project.basePath.toString())
                }
                coroutineContext.ensureActive()
                component.refreshModel(results, getCellRenderer())
            } finally {
                component.fileList.setPaintBusy(false)
            }
        }
    }

    /**
     * Run the command and collect the output to a string variable with a limited size
     * @see MAX_OUTPUT_SIZE
     */
    protected open suspend fun runCommand(commands: List<String>, projectBasePath: String): String? {
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
     * @see MAX_NUMBER_OR_RESULTS
     */
    protected open suspend fun runCommand(
        commands: List<String>,
        listModel: DefaultListModel<FuzzyContainer>,
        projectBasePath: String
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
                            val rowContainer = RowContainer.rowContainerFromString(line, projectBasePath, useRg)
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

    private suspend fun findInFiles(
        searchString: String,
        projectBasePath: String
    ): ListModel<FuzzyContainer> {
        val listModel = DefaultListModel<FuzzyContainer>()
        if (useRg) {
            val secondary = (component as FuzzyFinderComponent).getSecondaryText().trim()
            val commands = mutableListOf(
                "rg",
                "--no-heading",
                "--color=never",
                "-n",
                "--with-filename",
                "--column"
            )
            if (secondary.isNotEmpty()) {
                val ext = secondary.removePrefix(".")
                val glob = "*.${ext}"
                commands.addAll(listOf("-g", glob))
            }
            commands.addAll(listOf(searchString, "."))
            runCommand(commands, listModel, projectBasePath)
        } else {
            if (isWindows) {
                runCommand(listOf("findstr", "/p", "/s", "/n", searchString, "*"), listModel, projectBasePath)
            } else {
                runCommand(listOf("grep", "--color=none", "-r", "-n", searchString, "."), listModel, projectBasePath)
            }
        }

        return listModel
    }

    private fun createListeners(project: Project) {
        // Add a listener that updates the contents of the preview pane
        component.fileList.addListSelectionListener { event ->
            if (!event.valueIsAdjusting) {
                if (component.fileList.isEmpty) {
                    actionScope?.launch(Dispatchers.EDT) {
                        defaultDoc?.let { (component as FuzzyFinderComponent).previewPane.updateFile(it) }
                    }
                    return@addListSelectionListener
                }
                val selectedValue = component.fileList.selectedValue
                val fileUrl = "file://${selectedValue?.getFileUri()}"

                actionScope?.launch(Dispatchers.Default) {
                    val file = withContext(Dispatchers.IO) {
                        VirtualFileManager.getInstance().findFileByUrl(fileUrl)
                    }

                    file?.let {
                        (component as FuzzyFinderComponent).previewPane.coUpdateFile(
                            file,
                            (selectedValue as RowContainer).rowNumber
                        )
                    }
                }
            }
        }

        ActivationBindings.install(
            component,
            onActivate = { handleInput(project) }
        )
    }

    private fun handleInput(project: Project) {
        val selectedValue = component.fileList.selectedValue
        val virtualFile =
            VirtualFileManager.getInstance().findFileByUrl("file://${selectedValue?.getFileUri()}")
        virtualFile?.let {
            openFile(project, selectedValue, it)
        }
    }

    private fun openFile(project: Project, fuzzyContainer: FuzzyContainer?, virtualFile: VirtualFile) {
        val fileEditorManager = FileEditorManager.getInstance(project)
        val currentEditor = fileEditorManager.selectedTextEditor
        val previousFile = currentEditor?.virtualFile

        if (fileEditorManager.isFileOpen(virtualFile)) {
            fileEditorManager.openFile(virtualFile, true)
        } else {
            fileEditorManager.openFile(virtualFile, true)
            if (currentEditor != null && !globalState.newTab) {
                fileEditorManager.selectedEditor?.let {
                    if (previousFile != null) {
                        fileEditorManager.closeFile(previousFile)
                    }
                }
            }
        }
        popup.cancel()
        ApplicationManager.getApplication().invokeLater {
            val rc = fuzzyContainer as RowContainer
            val lp = LogicalPosition(rc.rowNumber, rc.columnNumber)
            val editor = fileEditorManager.selectedTextEditor
            editor?.scrollingModel?.scrollTo(lp, ScrollType.CENTER)
            editor?.caretModel?.moveToLogicalPosition(lp)
        }
    }
}
