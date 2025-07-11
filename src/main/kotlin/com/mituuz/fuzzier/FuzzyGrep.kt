/*

 MIT License

 Copyright (c) 2025 Mitja Leino

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all
 copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 SOFTWARE.

 */

package com.mituuz.fuzzier

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
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
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupListener
import com.intellij.openapi.ui.popup.LightweightWindowEvent
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.mituuz.fuzzier.components.FuzzyFinderComponent
import com.mituuz.fuzzier.entities.FuzzyContainer
import com.mituuz.fuzzier.entities.RowContainer
import kotlinx.coroutines.*
import org.apache.commons.lang3.StringUtils
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import javax.swing.AbstractAction
import javax.swing.DefaultListModel
import javax.swing.JComponent
import javax.swing.KeyStroke
import kotlin.coroutines.cancellation.CancellationException

open class FuzzyGrep() : FuzzyAction() {
    companion object {
        const val FUZZIER_NOTIFICATION_GROUP: String = "Fuzzier Notification Group"
    }

    override var popupTitle: String = "Fuzzy Grep"
    override var dimensionKey = "FuzzyGrepPopup"
    private var lock = ReentrantLock()
    var useRg = true
    val isWindows = System.getProperty("os.name").lowercase().contains("win")
    var currentJob: Job? = null

    override fun runAction(
        project: Project,
        actionEvent: AnActionEvent
    ) {
        currentJob?.cancel()
        setCustomHandlers()

        val projectBasePath = project.basePath.toString()
        currentJob = CoroutineScope(Dispatchers.EDT).launch {
            val rgCommand = checkInstallation("rg", projectBasePath)
            if (rgCommand != null) {
                showNotification(
                    "No `rg` command found",
                    """
                    No ripgrep found with command: $rgCommand<br>
                    Fallback to `grep` or `findstr`<br>
                    This notification can be disabled
                """.trimIndent(),
                    project,
                    NotificationType.WARNING
                )

                if (isWindows) {
                    val findstrCommand = checkInstallation("findstr", projectBasePath)
                    if (findstrCommand != null) {
                        showNotification(
                            "No `findstr` command found",
                            "No findstr found with command: $findstrCommand",
                            project
                        )
                        return@launch
                    }
                    popupTitle = "Fuzzy Grep (findstr)"
                } else {
                    val grepCommand = checkInstallation("grep", projectBasePath)
                    if (grepCommand != null) {
                        showNotification(
                            "No `grep` command found",
                            "No grep found with command: $grepCommand",
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
        }

        ApplicationManager.getApplication().invokeLater {
            defaultDoc = EditorFactory.getInstance().createDocument("")
            component = FuzzyFinderComponent(project)
            createListeners(project)
            createSharedListeners(project)

            showPopup(project)

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

    override fun createPopup(screenDimensionKey: String): JBPopup {
        val popup = getInitialPopup(screenDimensionKey)

        popup.addListener(object : JBPopupListener {
            override fun onClosed(event: LightweightWindowEvent) {
                globalState.splitPosition =
                    (component as FuzzyFinderComponent).splitPane.dividerLocation
                resetOriginalHandlers()
                super.onClosed(event)
                currentTask?.cancel(true)
            }
        })

        return popup
    }

    /**
     * OS-specific to see if a specific executable is found
     * @return the used command if no installation detected, otherwise null
     */
    private suspend fun checkInstallation(executable: String, projectBasePath: String): String? {
        val command = if (isWindows) {
            listOf("where", executable)
        } else {
            listOf("which", executable)
        }

        val result = withContext(Dispatchers.IO) { runCommand(command, projectBasePath) }
        if (result.isNullOrBlank() || result.contains("Could not find files")) {
            return command.joinToString(" ")
        }

        return null
    }

    override fun updateListContents(project: Project, searchString: String) {
        if (StringUtils.isBlank(searchString)) {
            component.fileList.model = DefaultListModel()
            return
        }

        currentTask?.takeIf { !it.isDone }?.cancel(true)
        if (lock.tryLock(500, TimeUnit.MILLISECONDS)) {

            try {
                currentTask = ApplicationManager.getApplication().executeOnPooledThread {
                    try {
                        val task = currentTask

                        if (task?.isCancelled == true) return@executeOnPooledThread

                        component.fileList.setPaintBusy(true)
                        val listModel = DefaultListModel<FuzzyContainer>()

                        if (task?.isCancelled == true) return@executeOnPooledThread

                        findInFiles(searchString, listModel, project.basePath.toString())

                        if (task?.isCancelled == true) return@executeOnPooledThread

                        ApplicationManager.getApplication().invokeLater {
                            synchronized(component.fileList.model) {
                                component.fileList.model = listModel
                                component.fileList.cellRenderer = getCellRenderer()
                                if (!listModel.isEmpty) {
                                    component.fileList.selectedIndex = 0
                                }
                                component.fileList.setPaintBusy(false)
                            }
                        }
                    } catch (_: InterruptedException) {
                        return@executeOnPooledThread
                    } catch (_: CancellationException) {
                        return@executeOnPooledThread
                    }
                }
                try {
                    currentTask?.get()
                } catch (_: InterruptedException) {
                } catch (_: CancellationException) {
                }
            } finally {
                lock.unlock()
            }
        }
    }

    protected open fun runCommand(commands: List<String>, projectBasePath: String): String? {
        return try {
            val commandLine = GeneralCommandLine(commands)
                .withWorkDirectory(projectBasePath)
                .withRedirectErrorStream(true)
            val output = StringBuilder()
            val processHandler = OSProcessHandler(commandLine)
            processHandler.addProcessListener(object : ProcessAdapter() {
                override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
                    output.appendLine(event.text.replace("\n", ""))
                }
            })
            processHandler.startNotify()
            processHandler.waitFor(2000)
            output.toString()
        } catch (_: IOException) {
            null
        } catch (_: InterruptedException) {
            null
        }
    }

    private fun findInFiles(
        searchString: String, listModel: DefaultListModel<FuzzyContainer>,
        projectBasePath: String
    ) {

        val res = if (useRg) {
            runCommand(
                listOf(
                    "rg",
                    "--no-heading",
                    "--color=never",
                    "-n",
                    "--with-filename",
                    "--column",
                    searchString,
                    "."
                ), projectBasePath
            )
        } else {
            if (isWindows) {
                runCommand(listOf("findstr", "/p", "/s", "/n", searchString, "*"), projectBasePath)
            } else {
                runCommand(listOf("grep", "--color=none", "-r", "-n", searchString, "."), projectBasePath)
            }
        }

        if (res != null) {
            res.lines()
                .forEach { line ->
                    val rowContainer = RowContainer.rowContainerFromString(line, projectBasePath, useRg)
                    if (rowContainer != null) {
                        listModel.addElement(rowContainer)
                    }
                }
        } else {
            return
        }
    }

    private fun createListeners(project: Project) {
        // Add a listener that updates the contents of the preview pane
        component.fileList.addListSelectionListener { event ->
            if (!event.valueIsAdjusting) {
                if (component.fileList.isEmpty) {
                    ApplicationManager.getApplication().invokeLater {
                        defaultDoc?.let { (component as FuzzyFinderComponent).previewPane.updateFile(it) }
                    }
                    return@addListSelectionListener
                }
                val selectedValue = component.fileList.selectedValue
                val fileUrl = "file://${selectedValue?.getFileUri()}"

                ProgressManager.getInstance().run(object : Task.Backgroundable(null, "Loading file", false) {
                    override fun run(indicator: ProgressIndicator) {
                        val file = VirtualFileManager.getInstance().findFileByUrl(fileUrl)
                        file?.let {
                            (component as FuzzyFinderComponent).previewPane.updateFile(
                                file,
                                (selectedValue as RowContainer).rowNumber
                            )
                        }
                    }
                })
            }
        }

        // Add a mouse listener for double-click
        component.fileList.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 2) {
                    val selectedValue = component.fileList.selectedValue
                    val virtualFile =
                        VirtualFileManager.getInstance().findFileByUrl("file://${selectedValue?.getFileUri()}")
                    // Open the file in the editor
                    virtualFile?.let {
                        openFile(project, selectedValue, it)
                    }
                }
            }
        })

        // Add a listener that opens the currently selected file when pressing enter (focus on the text box)
        val enterKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0)
        val enterActionKey = "openFile"
        val inputMap = component.searchField.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
        inputMap.put(enterKeyStroke, enterActionKey)
        component.searchField.actionMap.put(enterActionKey, object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent?) {
                val selectedValue = component.fileList.selectedValue
                val virtualFile =
                    VirtualFileManager.getInstance().findFileByUrl("file://${selectedValue?.getFileUri()}")
                virtualFile?.let {
                    openFile(project, selectedValue, it)
                }
            }
        })
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
