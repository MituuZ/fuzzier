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

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.rootManager
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupListener
import com.intellij.openapi.ui.popup.LightweightWindowEvent
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.mituuz.fuzzier.components.SimpleFinderComponent
import com.mituuz.fuzzier.entities.FuzzyContainer
import com.mituuz.fuzzier.entities.StringEvaluator
import com.mituuz.fuzzier.util.FuzzierUtil
import org.apache.commons.lang3.StringUtils
import java.awt.event.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future
import javax.swing.*
import kotlin.coroutines.cancellation.CancellationException

class FuzzyMover : FuzzyAction() {
    override var popupTitle = "Fuzzy File Mover"
    override var dimensionKey = "FuzzyMoverPopup"
    lateinit var movableFile: PsiFile
    lateinit var currentFile: VirtualFile

    override fun runAction(project: Project, actionEvent: AnActionEvent) {
        setCustomHandlers()

        ApplicationManager.getApplication().invokeLater {
            component = SimpleFinderComponent()
            createListeners(project)
            createSharedListeners(project)

            val currentEditor = FileEditorManager.getInstance(project).selectedTextEditor
            if (currentEditor != null) {
                currentFile = currentEditor.virtualFile
                component.fileList.setEmptyText("Press enter to use current file: ${currentFile.path}")
            }

            showPopup(project)
        }
    }

    override fun createPopup(screenDimensionKey: String): JBPopup {
        val popup = getInitialPopup(screenDimensionKey)

        popup.addListener(object : JBPopupListener {
            override fun onClosed(event: LightweightWindowEvent) {
                resetOriginalHandlers()
                super.onClosed(event)
            }
        })

        return popup
    }

    private fun createListeners(project: Project) {
        // Add a mouse listener for double-click
        component.fileList.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 2) {
                    handleInput(project)
                }
            }
        })

        val enterKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0)
        val enterActionKey = "openFile"
        val inputMap = component.searchField.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
        inputMap.put(enterKeyStroke, enterActionKey)
        component.searchField.actionMap.put(enterActionKey, object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent?) {
                handleInput(project)
            }
        })
    }

    fun handleInput(project: Project): CompletableFuture<Unit> {
        val completableFuture = CompletableFuture<Unit>()
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
                completableFuture.complete(null)
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
                        completableFuture.complete(null)
                    } else {
                        completableFuture.complete(null)
                    }
                }
            }
        }
        return completableFuture
    }

    override fun updateListContents(project: Project, searchString: String) {
        if (StringUtils.isBlank(searchString)) {
            ApplicationManager.getApplication().invokeLater {
                component.fileList.model = DefaultListModel()
            }
            return
        }

        currentTask?.takeIf { !it.isDone }?.cancel(true)
        currentTask = ApplicationManager.getApplication().executeOnPooledThread {
            try {
                // Create a reference to the current task to check if it has been cancelled
                val task = currentTask
                component.fileList.setPaintBusy(true)
                var listModel = DefaultListModel<FuzzyContainer>()

                val stringEvaluator = getStringEvaluator()

                if (task?.isCancelled == true) return@executeOnPooledThread

                process(project, stringEvaluator, searchString, listModel, task)

                if (task?.isCancelled == true) return@executeOnPooledThread

                listModel = fuzzierUtil.sortAndLimit(listModel, true)

                if (task?.isCancelled == true) return@executeOnPooledThread

                ApplicationManager.getApplication().invokeLater {
                    component.fileList.model = listModel
                    component.fileList.cellRenderer = getCellRenderer()
                    component.fileList.setPaintBusy(false)
                    if (!component.fileList.isEmpty) {
                        component.fileList.setSelectedValue(listModel[0], true)
                    }
                }
            } catch (_: InterruptedException) {
                return@executeOnPooledThread
            } catch (_: CancellationException) {
                return@executeOnPooledThread
            }
        }
    }

    private fun getStringEvaluator(): StringEvaluator {
        return StringEvaluator(
            projectState.exclusionSet,
            projectState.modules
        )
    }
    
    private fun process(project: Project, stringEvaluator: StringEvaluator, searchString: String,
                        listModel: DefaultListModel<FuzzyContainer>, task: Future<*>?) {
        val moduleManager = ModuleManager.getInstance(project)
        val ss = FuzzierUtil.cleanSearchString(searchString, projectState.ignoredCharacters)
        if (projectState.isProject) {
            processProject(project, stringEvaluator, ss, listModel, task)
        } else {
            processModules(moduleManager, stringEvaluator, ss, listModel, task)
        }
    }

    private fun processProject(project: Project, stringEvaluator: StringEvaluator,
                               searchString: String, listModel: DefaultListModel<FuzzyContainer>, task: Future<*>?) {
        val contentIterator = if (!component.isDirSelector) {
            stringEvaluator.getContentIterator(project.name, searchString, listModel, task)
        } else {
            stringEvaluator.getDirIterator(project.name, searchString, listModel, task)
        }
        ProjectFileIndex.getInstance(project).iterateContent(contentIterator)
    }

    private fun processModules(moduleManager: ModuleManager, stringEvaluator: StringEvaluator,
                               searchString: String, listModel: DefaultListModel<FuzzyContainer>, task: Future<*>?) {
        for (module in moduleManager.modules) {
            val moduleFileIndex = module.rootManager.fileIndex

            val contentIterator = if (!component.isDirSelector) {
                stringEvaluator.getContentIterator(module.name, searchString, listModel, task)
            } else {
                stringEvaluator.getDirIterator(module.name, searchString, listModel, task)
            }
            moduleFileIndex.iterateContent(contentIterator)
        }
    }
}