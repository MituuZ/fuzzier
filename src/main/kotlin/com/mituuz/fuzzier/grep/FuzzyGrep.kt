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

package com.mituuz.fuzzier.grep

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
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.util.SingleAlarm
import com.mituuz.fuzzier.actions.FuzzyAction
import com.mituuz.fuzzier.components.FuzzyFinderComponent
import com.mituuz.fuzzier.entities.CaseMode
import com.mituuz.fuzzier.entities.FuzzyContainer
import com.mituuz.fuzzier.entities.GrepConfig
import com.mituuz.fuzzier.entities.RowContainer
import com.mituuz.fuzzier.grep.backend.BackendResolver
import com.mituuz.fuzzier.grep.backend.BackendStrategy
import com.mituuz.fuzzier.intellij.files.FileOpeningUtil
import com.mituuz.fuzzier.runner.DefaultCommandRunner
import com.mituuz.fuzzier.ui.bindings.ActivationBindings
import com.mituuz.fuzzier.ui.popup.PopupConfig
import com.mituuz.fuzzier.ui.preview.CoroutinePreviewAlarmProvider
import com.mituuz.fuzzier.ui.preview.PreviewAlarmProvider
import kotlinx.coroutines.*
import org.apache.commons.lang3.StringUtils
import javax.swing.DefaultListModel
import javax.swing.ListModel

open class FuzzyGrep : FuzzyAction() {
    companion object {
        const val FUZZIER_NOTIFICATION_GROUP: String = "Fuzzier Notification Group"
    }

    val isWindows = System.getProperty("os.name").lowercase().contains("win")
    private val backendResolver = BackendResolver(isWindows)
    private val commandRunner = DefaultCommandRunner()
    private var currentLaunchJob: Job? = null
    private var backend: BackendStrategy? = null
    private var previewAlarm: SingleAlarm? = null
    private var previewAlarmProvider: PreviewAlarmProvider? = null
    private lateinit var grepConfig: GrepConfig

    open fun getGrepConfig(project: Project): GrepConfig {
        return GrepConfig(
            targets = listOf("."),
            caseMode = CaseMode.SENSITIVE,
            title = "Fuzzy Grep",
        )
    }

    override fun runAction(
        project: Project, actionEvent: AnActionEvent
    ) {
        currentLaunchJob?.cancel()
        grepConfig = getGrepConfig(project)
        val popupTitle = grepConfig.getPopupTitle()

        val projectBasePath = project.basePath.toString()
        currentLaunchJob = actionScope?.launch(Dispatchers.EDT) {
            val backendResult: Result<BackendStrategy> = backendResolver.resolveBackend(commandRunner, projectBasePath)
            backend = backendResult.getOrNull()

            if (backendResult.isFailure) {
                showNotification(
                    "No search command found", "Fuzzy Grep failed: no suitable grep command found", project
                )
                return@launch
            }
            if (backend == null) return@launch

            yield()
            defaultDoc = EditorFactory.getInstance().createDocument("")
            val showSecondaryField = backend!!.supportsSecondaryField() && grepConfig.supportsSecondaryField
            component = FuzzyFinderComponent(
                project = project,
                showSecondaryField = showSecondaryField
            )
            previewAlarmProvider = CoroutinePreviewAlarmProvider(actionScope)
            previewAlarm = previewAlarmProvider?.getPreviewAlarm(component, defaultDoc)
            createListeners(project)
            val maybePopup = getPopupProvider().show(
                project = project,
                content = component,
                focus = component.searchField,
                config = PopupConfig(
                    title = popupTitle,
                    preferredSizeProvider = component.preferredSize,
                    dimensionKey = "FuzzyGrepPopup",
                    resetWindow = { globalState.resetWindow },
                    clearResetWindowFlag = { globalState.resetWindow = false }),
                cleanupFunction = { cleanupPopup() },
            )

            if (maybePopup == null) return@launch
            popup = maybePopup

            createSharedListeners(project)
        }
    }

    private fun showNotification(
        title: String, content: String, project: Project, type: NotificationType = NotificationType.ERROR
    ) {
        val grepNotification = Notification(
            FUZZIER_NOTIFICATION_GROUP, title, content, type
        )
        Notifications.Bus.notify(grepNotification, project)
    }

    override fun onPopupClosed() {
        previewAlarm?.dispose()
        currentLaunchJob?.cancel()
        currentLaunchJob = null
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
                    findInFiles(
                        searchString,
                        project
                    )
                }
                coroutineContext.ensureActive()
                component.refreshModel(results, getCellRenderer())
            } finally {
                component.fileList.setPaintBusy(false)
            }
        }
    }

    private suspend fun findInFiles(
        searchString: String,
        project: Project,
    ): ListModel<FuzzyContainer> {
        val listModel = DefaultListModel<FuzzyContainer>()
        val projectBasePath = project.basePath.toString()

        if (backend != null) {
            val secondaryFieldText = (component as FuzzyFinderComponent).getSecondaryText()
            val commands = backend!!.buildCommand(grepConfig, searchString, secondaryFieldText)
            commandRunner.runCommandPopulateListModel(commands, listModel, projectBasePath, backend!!)
        }

        return listModel
    }

    private fun createListeners(project: Project) {
        // Add a listener that updates the contents of the preview pane
        component.fileList.addListSelectionListener { event ->
            if (event.valueIsAdjusting) {
                return@addListSelectionListener
            } else {
                previewAlarm?.cancelAndRequest()
            }
        }

        ActivationBindings.install(
            component, onActivate = { handleInput(project) })
    }

    private fun handleInput(project: Project) {
        val selectedValue = component.fileList.selectedValue
        val virtualFile =
            VirtualFileManager.getInstance().findFileByUrl("file://${selectedValue?.getFileUri()}")
        virtualFile?.let {
            val fileEditorManager = FileEditorManager.getInstance(project)

            FileOpeningUtil.openFile(
                fileEditorManager,
                virtualFile,
                globalState.newTab
            ) {
                popup.cancel()
                ApplicationManager.getApplication().invokeLater {
                    val rc = selectedValue as RowContainer
                    val lp = LogicalPosition(rc.rowNumber, rc.columnNumber)
                    val editor = fileEditorManager.selectedTextEditor
                    editor?.scrollingModel?.scrollTo(lp, ScrollType.CENTER)
                    editor?.caretModel?.moveToLogicalPosition(lp)
                }
            }
        }
    }
}
