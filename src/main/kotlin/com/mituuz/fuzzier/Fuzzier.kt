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

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.impl.EditorHistoryManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.util.SingleAlarm
import com.mituuz.fuzzier.actions.filesystem.FilesystemAction
import com.mituuz.fuzzier.components.FuzzyFinderComponent
import com.mituuz.fuzzier.entities.FuzzyContainer
import com.mituuz.fuzzier.settings.FuzzierGlobalSettingsService
import com.mituuz.fuzzier.ui.bindings.ActivationBindings
import com.mituuz.fuzzier.ui.popup.DefaultPopupProvider
import com.mituuz.fuzzier.ui.popup.PopupConfig
import com.mituuz.fuzzier.util.InitialViewHandler
import javax.swing.DefaultListModel

open class Fuzzier : FilesystemAction() {
    private var previewAlarm: SingleAlarm? = null
    private var lastPreviewKey: String? = null
    private val popupProvider = DefaultPopupProvider()
    protected open var popupTitle = "Fuzzy Search"

    override fun buildFileFilter(project: Project): (VirtualFile) -> Boolean =
        { vf -> !vf.isDirectory }

    override fun runAction(project: Project, actionEvent: AnActionEvent) {
        ApplicationManager.getApplication().invokeLater {
            defaultDoc = EditorFactory.getInstance().createDocument("")
            component = FuzzyFinderComponent(project)
            previewAlarm = getPreviewAlarm()
            createListeners(project)
            val maybePopup = popupProvider.show(
                project = project,
                content = component,
                focus = component.searchField,
                config = PopupConfig(
                    title = popupTitle,
                    preferredSizeProvider = component.preferredSize,
                    dimensionKey = "FuzzySearchPopup",
                    resetWindow = { globalState.resetWindow },
                    clearResetWindowFlag = { globalState.resetWindow = false }
                ),
                cleanupFunction = { cleanupPopup() }
            )

            if (maybePopup == null) return@invokeLater
            popup = maybePopup

            createSharedListeners(project)

            (component as FuzzyFinderComponent).splitPane.dividerLocation =
                globalState.splitPosition

            if (globalState.recentFilesMode != FuzzierGlobalSettingsService.RecentFilesMode.NONE) {
                createInitialView(project)
            }
        }
    }

    override fun onPopupClosed() {
        globalState.splitPosition =
            (component as FuzzyFinderComponent).splitPane.dividerLocation
        previewAlarm?.dispose()
        lastPreviewKey = null
    }

    override fun handleEmptySearchString(project: Project) {
        if (globalState.recentFilesMode != FuzzierGlobalSettingsService.RecentFilesMode.NONE) {
            createInitialView(project)
        } else {
            ApplicationManager.getApplication().invokeLater {
                component.fileList.model = DefaultListModel()
                defaultDoc?.let { (component as FuzzyFinderComponent).previewPane.updateFile(it) }
            }
        }
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

    private fun createInitialView(project: Project) {
        component.fileList.setPaintBusy(true)
        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                val editorHistoryManager = EditorHistoryManager.getInstance(project)

                val listModel = when (globalState.recentFilesMode) {
                    FuzzierGlobalSettingsService.RecentFilesMode.RECENT_PROJECT_FILES -> InitialViewHandler.Companion.getRecentProjectFiles(
                        globalState,
                        fuzzierUtil,
                        editorHistoryManager,
                        project
                    )

                    FuzzierGlobalSettingsService.RecentFilesMode.RECENTLY_SEARCHED_FILES -> InitialViewHandler.Companion.getRecentlySearchedFiles(
                        projectState
                    )

                    else -> {
                        DefaultListModel<FuzzyContainer>()
                    }
                }

                ApplicationManager.getApplication().invokeLater {
                    component.refreshModel(listModel, getCellRenderer())
                }
            } finally {
                component.fileList.setPaintBusy(false)
            }
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
        if (fuzzyContainer != null) {
            InitialViewHandler.addFileToRecentlySearchedFiles(fuzzyContainer, projectState, globalState)
        }
        popup.cancel()
    }

    private fun getPreviewAlarm(): SingleAlarm {
        return SingleAlarm(
            {
                val fuzzyFinderComponent = (component as FuzzyFinderComponent)
                val selected = component.fileList.selectedValue

                if (selected == null || component.fileList.isEmpty) {
                    defaultDoc?.let { fuzzyFinderComponent.previewPane.updateFile(it) }
                    lastPreviewKey = null
                    return@SingleAlarm
                }

                val fileUrl = "file://${selected.getFileUri()}"
                if (fileUrl == lastPreviewKey) return@SingleAlarm
                lastPreviewKey = fileUrl

                val vf = VirtualFileManager.getInstance().findFileByUrl(fileUrl)
                if (vf != null) fuzzyFinderComponent.previewPane.updateFile(vf)
            },
            75,
        )
    }
}