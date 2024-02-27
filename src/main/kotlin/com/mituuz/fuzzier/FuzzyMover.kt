package com.mituuz.fuzzier

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.EditorActionHandler
import com.intellij.openapi.editor.actionSystem.EditorActionManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.JBPopupListener
import com.intellij.openapi.ui.popup.LightweightWindowEvent
import com.intellij.openapi.util.DimensionService
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.wm.WindowManager
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.refactoring.move.moveFilesOrDirectories.MoveFilesOrDirectoriesUtil
import com.mituuz.fuzzier.components.SimpleFinderComponent
import com.mituuz.fuzzier.settings.FuzzierSettingsService
import java.awt.event.*
import java.util.concurrent.CompletableFuture
import javax.swing.AbstractAction
import javax.swing.JComponent
import javax.swing.KeyStroke
import javax.swing.SwingUtilities

class FuzzyMover : AnAction() {
    lateinit var component: SimpleFinderComponent
    private var fuzzierSettingsService = service<FuzzierSettingsService>()
    private var popup: JBPopup? = null
    private val dimensionKey: String = "FuzzyMoverPopup"
    private lateinit var originalDownHandler: EditorActionHandler
    private lateinit var originalUpHandler: EditorActionHandler
    lateinit var movableFile: PsiFile
    lateinit var currentFile: String

    override fun actionPerformed(p0: AnActionEvent) {
        setCustomHandlers()
        SwingUtilities.invokeLater {
            p0.project?.let { project ->
                component = SimpleFinderComponent(project)
                val projectBasePath = project.basePath
                if (projectBasePath != null) {
                    createListeners(project, projectBasePath)
                }

                val mainWindow = WindowManager.getInstance().getIdeFrame(p0.project)?.component
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

    fun resetOriginalHandlers() {
        val actionManager = EditorActionManager.getInstance()
        actionManager.setActionHandler(IdeActions.ACTION_EDITOR_MOVE_CARET_DOWN, originalDownHandler)
        actionManager.setActionHandler(IdeActions.ACTION_EDITOR_MOVE_CARET_UP, originalUpHandler)
    }

    private fun setCustomHandlers() {
        val actionManager = EditorActionManager.getInstance()
        originalDownHandler = actionManager.getActionHandler(IdeActions.ACTION_EDITOR_MOVE_CARET_DOWN)
        originalUpHandler = actionManager.getActionHandler(IdeActions.ACTION_EDITOR_MOVE_CARET_UP)

        actionManager.setActionHandler(IdeActions.ACTION_EDITOR_MOVE_CARET_DOWN, FuzzyListActionHandler(this, false))
        actionManager.setActionHandler(IdeActions.ACTION_EDITOR_MOVE_CARET_UP, FuzzyListActionHandler(this, true))
    }

    fun moveListUp() {
        val selectedIndex = component.fileList.selectedIndex
        if (selectedIndex > 0) {
            component.fileList.selectedIndex = selectedIndex - 1
            component.fileList.ensureIndexIsVisible(selectedIndex - 1)
        }
    }

    fun moveListDown() {
        val selectedIndex = component.fileList.selectedIndex
        val length = component.fileList.model.size
        if (selectedIndex < length - 1) {
            component.fileList.selectedIndex = selectedIndex + 1
            component.fileList.ensureIndexIsVisible(selectedIndex + 1)
        }
    }

    class FuzzyListActionHandler(private val fuzzyMover: FuzzyMover, private val isUp: Boolean) :
        EditorActionHandler() {
        override fun doExecute(editor: Editor, caret: Caret?, dataContext: DataContext?) {
            if (isUp) {
                fuzzyMover.moveListUp()
            } else {
                fuzzyMover.moveListDown()
            }

            super.doExecute(editor, caret, dataContext)
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

        // Add a listener that opens the currently selected file when pressing enter (focus on the text box)
        val enterKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0)
        val enterActionKey = "openFile"
        val inputMap = component.searchField.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
        inputMap.put(enterKeyStroke, enterActionKey)
        component.searchField.actionMap.put(enterActionKey, object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent?) {
                handleInput(projectBasePath, project)
            }
        })

        // Add a listener to move fileList up and down by using CTRL + k/j
        val kShiftKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_K, InputEvent.CTRL_DOWN_MASK)
        val jShiftKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_J, InputEvent.CTRL_DOWN_MASK)
        inputMap.put(kShiftKeyStroke, "moveUp")
        component.searchField.actionMap.put("moveUp", object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent?) {
                moveListUp()
            }
        })
        inputMap.put(jShiftKeyStroke, "moveDown")
        component.searchField.actionMap.put("moveDown", object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent?) {
                moveListDown()
            }
        })
    }

    fun handleInput(projectBasePath: String, project: Project): CompletableFuture<Void> {
        val completableFuture = CompletableFuture<Void>()
        var selectedValue = component.fileList.selectedValue
        if (selectedValue == null) {
            selectedValue = currentFile
        }
        if (!component.isDirSelector) {
            val virtualFile =
                VirtualFileManager.getInstance().findFileByUrl("file://$projectBasePath$selectedValue")
            virtualFile?.let {
                ApplicationManager.getApplication().executeOnPooledThread() {
                    ApplicationManager.getApplication().runReadAction {
                        movableFile = PsiManager.getInstance(project).findFile(it)!!
                    }
                }
            }
            SwingUtilities.invokeLater {
                component.isDirSelector = true
                component.searchField.text = ""
                component.fileList.setEmptyText("Select target folder")
            }
            completableFuture.complete(null)
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
}