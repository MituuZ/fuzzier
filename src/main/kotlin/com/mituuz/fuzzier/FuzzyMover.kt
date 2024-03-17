package com.mituuz.fuzzier

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.JBPopupListener
import com.intellij.openapi.ui.popup.LightweightWindowEvent
import com.intellij.openapi.util.DimensionService
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.wm.WindowManager
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.codeStyle.extractor.ui.ExtractedSettingsDialog.CellRenderer
import com.intellij.refactoring.move.moveFilesOrDirectories.MoveFilesOrDirectoriesUtil
import com.mituuz.fuzzier.StringEvaluator.FilenameType
import com.mituuz.fuzzier.StringEvaluator.FilenameType.FILEPATH_ONLY
import com.mituuz.fuzzier.StringEvaluator.FuzzyMatchContainer
import com.mituuz.fuzzier.components.SimpleFinderComponent
import org.apache.commons.lang3.StringUtils
import java.awt.Component
import java.awt.event.*
import java.util.concurrent.CompletableFuture
import javax.swing.*

class FuzzyMover : FuzzyAction() {
    private val dimensionKey: String = "FuzzyMoverPopup"
    lateinit var movableFile: PsiFile
    lateinit var currentFile: String

    override fun actionPerformed(actionEvent: AnActionEvent) {
        setCustomHandlers()
        SwingUtilities.invokeLater {
            actionEvent.project?.let { project ->
                component = SimpleFinderComponent(project)
                val projectBasePath = project.basePath
                if (projectBasePath != null) {
                    createListeners(project, projectBasePath)
                    createSharedListeners(project)
                }

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

                    currentFile = projectBasePath?.let { it1 ->
                        FileEditorManager.getInstance(project).selectedTextEditor?.virtualFile?.path?.removePrefix(
                            it1
                        )
                    }.toString()
                    component.fileList.setEmptyText("Press enter to use current file:$currentFile")

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

    private fun createListeners(project: Project, projectBasePath: String) {
        // Add a mouse listener for double-click
        component.fileList.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 2) {
                    handleInput(projectBasePath, project)
                }
            }
        })

        val enterKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0)
        val enterActionKey = "openFile"
        val inputMap = component.searchField.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
        inputMap.put(enterKeyStroke, enterActionKey)
        component.searchField.actionMap.put(enterActionKey, object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent?) {
                handleInput(projectBasePath, project)
            }
        })
    }

    fun handleInput(projectBasePath: String, project: Project): CompletableFuture<Void> {
        val completableFuture = CompletableFuture<Void>()
        var selectedValue = component.fileList.selectedValue?.filePath
        if (selectedValue == null) {
            selectedValue = currentFile
        }
        if (!component.isDirSelector) {
            val virtualFile =
                VirtualFileManager.getInstance().findFileByUrl("file://$projectBasePath$selectedValue")
            virtualFile?.let {
                ApplicationManager.getApplication().executeOnPooledThread {
                    ApplicationManager.getApplication().runReadAction {
                        movableFile = PsiManager.getInstance(project).findFile(it)!!
                    }
                }
            }
            SwingUtilities.invokeLater {
                component.isDirSelector = true
                component.searchField.text = ""
                component.fileList.setEmptyText("Select target folder")
                completableFuture.complete(null)
            }
        } else {
            ApplicationManager.getApplication().invokeLater {
                val virtualDir =
                    VirtualFileManager.getInstance().findFileByUrl("file://$projectBasePath$selectedValue")
                val targetDirectory: CompletableFuture<PsiDirectory> = CompletableFuture.supplyAsync {
                    virtualDir?.let {
                        ApplicationManager.getApplication().runReadAction<PsiDirectory> {
                            PsiManager.getInstance(project).findDirectory(it)
                        }
                    }
                }

                targetDirectory.thenAcceptAsync { targetDir ->
                    val originalFilePath = movableFile.virtualFile.path.removePrefix(projectBasePath)
                    if (targetDir != null) {
                        WriteCommandAction.runWriteCommandAction(project) {
                            MoveFilesOrDirectoriesUtil.doMoveFile(movableFile, targetDir)
                        }
                        val targetLocation = virtualDir?.path?.removePrefix(projectBasePath)
                        val notification = Notification(
                            "Fuzzier Notification Group",
                            "File moved successfully",
                            "Moved $originalFilePath to $targetLocation/${movableFile.name}",
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
            SwingUtilities.invokeLater {
                component.fileList.model = DefaultListModel()
            }
            return
        }

        val stringEvaluator = StringEvaluator(fuzzierSettingsService.state.multiMatch,
            fuzzierSettingsService.state.exclusionList, fuzzierSettingsService.state.matchWeightSingleChar,
            fuzzierSettingsService.state.matchWeightStreakModifier,
            fuzzierSettingsService.state.matchWeightPartialPath)

        currentTask?.takeIf { !it.isDone }?.cancel(true)

        currentTask = ApplicationManager.getApplication().executeOnPooledThread {
            component.fileList.setPaintBusy(true)
            val listModel = DefaultListModel<FuzzyMatchContainer>()
            val projectFileIndex = ProjectFileIndex.getInstance(project)
            val projectBasePath = project.basePath

            val contentIterator = if (!component.isDirSelector) {
                projectBasePath?.let { stringEvaluator.getContentIterator(it, searchString, listModel) }
            } else {
                projectBasePath?.let { stringEvaluator.getDirIterator(it, searchString, listModel) }
            }

            if (contentIterator != null) {
                projectFileIndex.iterateContent(contentIterator)
            }
            val sortedList = listModel.elements().toList().sortedByDescending { it.score }
            listModel.clear()
            sortedList.forEach { listModel.addElement(it) }

            SwingUtilities.invokeLater {
                component.fileList.model = listModel
                component.fileList.cellRenderer = getCellRenderer()
                component.fileList.setPaintBusy(false)
                if (!component.fileList.isEmpty) {
                    component.fileList.setSelectedValue(listModel[0], true)
                }
            }
        }
    }

    fun getCellRenderer(): ListCellRenderer<Any?> {
        return object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(
                list: JList<*>?,
                value: Any?,
                index: Int,
                isSelected: Boolean,
                cellHasFocus: Boolean
            ): Component {
                val renderer =
                    super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus) as JLabel
                val container = value as FuzzyMatchContainer
                val filenameType: FilenameType = if (component.isDirSelector) {
                    FILEPATH_ONLY // Directories are always shown as full paths
                } else {
                    fuzzierSettingsService.state.filenameType
                }
                renderer.text = container.toString(filenameType)
                return renderer
            }
        }
    }
}