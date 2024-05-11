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

    override fun actionPerformed(actionEvent: AnActionEvent) {
        setCustomHandlers()
        ApplicationManager.getApplication().invokeLater {
            actionEvent.project?.let { project ->
                component = SimpleFinderComponent()
                createListeners(project)
                createSharedListeners(project)

                val mainWindow = WindowManager.getInstance().getIdeFrame(actionEvent.project)?.component
                mainWindow?.let {
                    popup = JBPopupFactory
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

                    currentFile = FileEditorManager.getInstance(project).selectedTextEditor?.virtualFile!!
                    component.fileList.setEmptyText("Press enter to use current file: ${currentFile.path}")

                    popup?.addListener(object : JBPopupListener {
                        override fun onClosed(event: LightweightWindowEvent) {
                            resetOriginalHandlers()
                            super.onClosed(event)
                        }
                    })
                    if (fuzzierSettingsService.state.resetWindow) {
                        DimensionService.getInstance().setSize(dimensionKey, null, project)
                        DimensionService.getInstance().setLocation(dimensionKey, null, project)
                        fuzzierSettingsService.state.resetWindow = false
                    }
                    popup!!.showInCenterOf(it)
                }
            }
        }
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
                val virtualDir =
                    VirtualFileManager.getInstance().findFileByUrl("file://$selectedValue")
                val targetDirectory: CompletableFuture<PsiDirectory> = CompletableFuture.supplyAsync {
                    virtualDir?.let {
                        ApplicationManager.getApplication().runReadAction<PsiDirectory> {
                            PsiManager.getInstance(project).findDirectory(it)
                        }
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
                            "Moved $originalFilePath to $selectedValue}",
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
            val isMultiModal = moduleManager.modules.size > 1

            for (module in moduleManager.modules) {
                val moduleFileIndex = module.rootManager.fileIndex
                var moduleBasePath = module.rootManager.contentRoots[0].path
                if (isMultiModal) {
                    moduleBasePath = moduleBasePath.substringBeforeLast("/")
                }
                state.modules[module.name] = moduleBasePath

                val contentIterator = if (!component.isDirSelector) {
                    stringEvaluator.getContentIterator(moduleBasePath, module.name, isMultiModal, searchString, listModel)
                } else {
                    stringEvaluator.getDirIterator(moduleBasePath, module.name, isMultiModal, searchString, listModel)
                }
                moduleFileIndex.iterateContent(contentIterator)
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
}