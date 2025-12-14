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

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupListener
import com.intellij.openapi.ui.popup.LightweightWindowEvent
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.mituuz.fuzzier.components.SimpleFinderComponent
import com.mituuz.fuzzier.ui.bindings.ActivationBindings
import com.mituuz.fuzzier.ui.popup.DefaultPopupProvider
import com.mituuz.fuzzier.ui.popup.PopupConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import java.util.concurrent.CompletableFuture
import javax.swing.DefaultListModel

class FuzzyMover : FilesystemAction() {
    override var popupTitle = "Fuzzy File Mover"
    override var dimensionKey = "FuzzyMoverPopup"
    lateinit var movableFile: PsiFile
    lateinit var currentFile: VirtualFile
    private val popupProvider = DefaultPopupProvider()

    override fun buildFileFilter(project: Project): (VirtualFile) -> Boolean {
        return { vf -> if (component.isDirSelector) vf.isDirectory else !vf.isDirectory }
    }

    override fun runAction(project: Project, actionEvent: AnActionEvent) {
        setCustomHandlers()

        actionScope?.cancel()
        actionScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        ApplicationManager.getApplication().invokeLater {
            component = SimpleFinderComponent()
            createListeners(project)
            val currentEditor = FileEditorManager.getInstance(project).selectedTextEditor
            if (currentEditor != null) {
                currentFile = currentEditor.virtualFile
                component.fileList.setEmptyText("Press enter to use current file: ${currentFile.path}")
            }

            // showPopup(project)
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
        }
    }

    fun createPopup() {
        popup.addListener(object : JBPopupListener {
            override fun onClosed(event: LightweightWindowEvent) {
                resetOriginalHandlers()

                currentUpdateListContentJob?.cancel()
                currentUpdateListContentJob = null

                actionScope?.cancel()
            }
        })
    }

    override fun handleEmptySearchString(project: Project) {
        ApplicationManager.getApplication().invokeLater {
            component.fileList.model = DefaultListModel()
        }
    }

    private fun createListeners(project: Project) {
        ActivationBindings.install(
            component,
            onActivate = { handleInput(project) }
        )
    }

    fun handleInput(project: Project) {
        var selectedValue = component.fileList.selectedValue?.getFileUri()
        if (selectedValue == null) {
            selectedValue = currentFile.path
        }
        if (!component.isDirSelector) {
            ApplicationManager.getApplication().executeOnPooledThread {
                val virtualFile = VirtualFileManager.getInstance().findFileByUrl("file://$selectedValue")
                ApplicationManager.getApplication().runReadAction {
                    virtualFile?.let {
                        movableFile = PsiManager.getInstance(project).findFile(it)!!
                    }
                }
            }
            ApplicationManager.getApplication().invokeLater {
                component.isDirSelector = true
                component.searchField.text = ""
                component.fileList.setEmptyText("Select target folder")
            }
        } else {
            ApplicationManager.getApplication().invokeLater {
                val targetDirectory: CompletableFuture<PsiDirectory> = CompletableFuture.supplyAsync {
                    ApplicationManager.getApplication().runReadAction<PsiDirectory> {
                        val virtualDir =
                            VirtualFileManager.getInstance().findFileByUrl("file://$selectedValue")
                        virtualDir?.let { PsiManager.getInstance(project).findDirectory(it) }
                    }
                }

                targetDirectory.thenAcceptAsync { targetDir ->
                    val originalFilePath = movableFile.virtualFile.path
                    if (targetDir != null) {
                        WriteCommandAction.runWriteCommandAction(project) {
                            movableFile.virtualFile.move(movableFile.manager, targetDir.virtualFile)
                        }
                        val notification = Notification(
                            "Fuzzier Notification Group",
                            "File moved successfully",
                            "Moved $originalFilePath to $selectedValue",
                            NotificationType.INFORMATION
                        )
                        Notifications.Bus.notify(notification, project)
                        ApplicationManager.getApplication().invokeLater {
                            popup.cancel()
                        }
                    }
                }
            }
        }
    }
}