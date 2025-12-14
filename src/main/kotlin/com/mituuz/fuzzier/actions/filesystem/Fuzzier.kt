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

package com.mituuz.fuzzier.actions.filesystem

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.impl.EditorHistoryManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupListener
import com.intellij.openapi.ui.popup.LightweightWindowEvent
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.util.SingleAlarm
import com.mituuz.fuzzier.components.FuzzyFinderComponent
import com.mituuz.fuzzier.entities.FuzzyContainer
import com.mituuz.fuzzier.settings.FuzzierGlobalSettingsService
import com.mituuz.fuzzier.ui.DefaultPopupProvider
import com.mituuz.fuzzier.ui.PopupConfig
import com.mituuz.fuzzier.util.InitialViewHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.AbstractAction
import javax.swing.DefaultListModel
import javax.swing.JComponent
import javax.swing.KeyStroke

open class Fuzzier : FilesystemAction() {
    override var popupTitle = "Fuzzy Search"
    override var dimensionKey = "FuzzySearchPopup"
    private var previewAlarm: SingleAlarm? = null
    private var lastPreviewKey: String? = null
    private val popupProvider = DefaultPopupProvider()

    override fun buildFileFilter(project: Project): (VirtualFile) -> Boolean =
        { vf -> !vf.isDirectory }

    override fun runAction(project: Project, actionEvent: AnActionEvent) {
        setCustomHandlers()

        actionScope?.cancel()
        actionScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        ApplicationManager.getApplication().invokeLater {
            defaultDoc = EditorFactory.getInstance().createDocument("")
            component = FuzzyFinderComponent(project)
            previewAlarm = getPreviewAlarm()
            createListeners(project)
            popup = popupProvider.show(
                project = project,
                content = component,
                focus = component.searchField,
                config = PopupConfig(
                    title = popupTitle,
                    preferredSizeProvider = component.preferredSize,
                    dimensionKey = dimensionKey,
                    resetWindow = { globalState.resetWindow },
                    clearResetWindowFlag = { globalState.resetWindow = false }
                )
            )
            createPopup()

            createSharedListeners(project)

            (component as FuzzyFinderComponent).splitPane.dividerLocation =
                globalState.splitPosition

            if (globalState.recentFilesMode != FuzzierGlobalSettingsService.RecentFilesMode.NONE) {
                createInitialView(project)
            }
        }
    }

    fun createPopup() {
        popup.addListener(object : JBPopupListener {
            override fun onClosed(event: LightweightWindowEvent) {
                globalState.splitPosition =
                    (component as FuzzyFinderComponent).splitPane.dividerLocation

                resetOriginalHandlers()

                currentUpdateListContentJob?.cancel()
                currentUpdateListContentJob = null

                actionScope?.cancel()

                previewAlarm?.dispose()
                lastPreviewKey = null
            }
        })
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

    private fun createInitialView(project: Project) {
        component.fileList.setPaintBusy(true)
        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                val editorHistoryManager = EditorHistoryManager.Companion.getInstance(project)

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
            InitialViewHandler.Companion.addFileToRecentlySearchedFiles(fuzzyContainer, projectState, globalState)
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