/*
MIT License

Copyright (c) 2024 Mitja Leino

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
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.rootManager
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.JBPopupListener
import com.intellij.openapi.ui.popup.LightweightWindowEvent
import com.intellij.openapi.util.DimensionService
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.wm.WindowManager
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.refactoring.move.moveFilesOrDirectories.MoveFilesOrDirectoriesUtil
import com.mituuz.fuzzier.components.SimpleFinderComponent
import com.mituuz.fuzzier.entities.FuzzyMatchContainer
import com.mituuz.fuzzier.settings.FuzzierSettingsService
import org.apache.commons.lang3.StringUtils
import java.awt.event.*
import java.util.concurrent.CompletableFuture
import javax.swing.*

class FuzzyMover : FuzzyAction() {
    private val dimensionKey: String = "FuzzyMoverPopup"
    lateinit var movableFile: PsiFile
    lateinit var currentFile: VirtualFile

    override fun runAction(project: Project, actionEvent: AnActionEvent) {
        setCustomHandlers()
        ApplicationManager.getApplication().invokeLater {
            component = SimpleFinderComponent()
            createListeners(project)
            createSharedListeners(project)

            val mainWindow = WindowManager.getInstance().getIdeFrame(actionEvent.project)?.component
            mainWindow?.let {
                popup = createPopup(project)

                val currentEditor = FileEditorManager.getInstance(project).selectedTextEditor
                if (currentEditor != null) {
                    currentFile = currentEditor.virtualFile
                    component.fileList.setEmptyText("Press enter to use current file: ${currentFile.path}")
                }

                if (fuzzierSettingsService.state.resetWindow) {
                    DimensionService.getInstance().setSize(dimensionKey, null, project)
                    DimensionService.getInstance().setLocation(dimensionKey, null, project)
                    fuzzierSettingsService.state.resetWindow = false
                }
                popup!!.showInCenterOf(it)
            }
        }
    }

    private fun createPopup(project: Project): JBPopup {
        val popup = JBPopupFactory
            .getInstance()
            .createComponentPopupBuilder(component, component.searchField)
            .setFocusable(true)
            .setRequestFocus(true)
            .setResizable(true)
            .setDimensionServiceKey(project, dimensionKey, true)
            .setTitle("Fuzzy File Mover")
            .setMovable(true)
            .setShowBorder(true)
            .createPopup()

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
                            MoveFilesOrDirectoriesUtil.doMoveFile(movableFile, targetDir)
                        }
                        val notification = Notification(
                            "Fuzzier Notification Group",
                            "File moved successfully",
                            "Moved $originalFilePath to $selectedValue",
                            NotificationType.INFORMATION
                        )
                        Notifications.Bus.notify(notification, project)
                        ApplicationManager.getApplication().invokeLater {
                            popup?.cancel()
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
            component.fileList.setPaintBusy(true)
            var listModel = DefaultListModel<FuzzyMatchContainer>()

            val stringEvaluator = StringEvaluator(
                fuzzierSettingsService.state.exclusionSet,
            )

            val state = service<FuzzierSettingsService>().state
            state.modules = HashMap()

            val moduleManager = ModuleManager.getInstance(project)
            if (fuzzierUtil.hasMultipleUniqueRootPaths(moduleManager)) {
                processModules(moduleManager, state, stringEvaluator, searchString, listModel)
            } else {
                processProject(project, state, stringEvaluator, searchString, listModel)
            }

            listModel = fuzzierUtil.sortAndLimit(listModel, true)

            ApplicationManager.getApplication().invokeLater {
                component.fileList.model = listModel
                component.fileList.cellRenderer = getCellRenderer()
                component.fileList.setPaintBusy(false)
                if (!component.fileList.isEmpty) {
                    component.fileList.setSelectedValue(listModel[0], true)
                }
            }
        }
    }

    private fun processProject(project: Project, state: FuzzierSettingsService.State, stringEvaluator: StringEvaluator,
                               searchString: String, listModel: DefaultListModel<FuzzyMatchContainer>) {
        val projectFileIndex = ProjectFileIndex.getInstance(project)
        val projectBasePath = project.basePath
        if (projectBasePath != null) {
            // state.modules[project.name] = projectBasePath

            val contentIterator = if (!component.isDirSelector) {
                stringEvaluator.getContentIterator(projectBasePath, project.name, false, searchString, listModel)
            } else {
                stringEvaluator.getDirIterator(projectBasePath, project.name, false, searchString, listModel)
            }

            projectFileIndex.iterateContent(contentIterator)
        }
    }

    private fun processModules(moduleManager: ModuleManager, state: FuzzierSettingsService.State,
                               stringEvaluator: StringEvaluator, searchString: String, listModel: DefaultListModel<FuzzyMatchContainer>) {
        for (module in moduleManager.modules) {
            val moduleFileIndex = module.rootManager.fileIndex
            val contentRoots = module.rootManager.contentRoots
            if (contentRoots.isNotEmpty()) {
                var moduleBasePath = contentRoots[0]?.path
                if (moduleBasePath != null) {
                    moduleBasePath = moduleBasePath.substringBeforeLast("/")
                    // state.modules[module.name] = moduleBasePath

                    val contentIterator = if (!component.isDirSelector) {
                        stringEvaluator.getContentIterator(moduleBasePath, module.name, true, searchString, listModel)
                    } else {
                        stringEvaluator.getDirIterator(moduleBasePath, module.name, true, searchString, listModel)
                    }
                    moduleFileIndex.iterateContent(contentIterator)
                }
            }
        }
    }
}