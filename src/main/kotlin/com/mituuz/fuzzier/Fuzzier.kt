package com.mituuz.fuzzier

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.rootManager
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.JBPopupListener
import com.intellij.openapi.ui.popup.LightweightWindowEvent
import com.intellij.openapi.util.DimensionService
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.wm.WindowManager
import com.mituuz.fuzzier.components.FuzzyFinderComponent
import com.mituuz.fuzzier.entities.FuzzyMatchContainer
import com.mituuz.fuzzier.settings.FuzzierSettingsService
import org.apache.commons.lang3.StringUtils
import java.awt.event.*
import java.util.HashMap
import javax.swing.*

open class Fuzzier : FuzzyAction() {
    private var defaultDoc: Document? = null
    open var title: String = "Fuzzy Search"
    private val fuzzyDimensionKey: String = "FuzzySearchPopup"
    // Used by FuzzierVCS to check if files are tracked by the VCS
    protected var changeListManager: ChangeListManager? = null

    override fun actionPerformed(actionEvent: AnActionEvent) {
        setCustomHandlers()
        ApplicationManager.getApplication().invokeLater {
            defaultDoc = EditorFactory.getInstance().createDocument("")
            actionEvent.project?.let { project ->
                component = FuzzyFinderComponent(project)
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
                        .setDimensionServiceKey(project, fuzzyDimensionKey, true)
                        .setTitle(title)
                        .setMovable(true)
                        .setShowBorder(true)
                        .createPopup()

                    popup?.addListener(object : JBPopupListener {
                        override fun onClosed(event: LightweightWindowEvent) {
                            fuzzierSettingsService.state.splitPosition =
                                (component as FuzzyFinderComponent).splitPane.dividerLocation
                            resetOriginalHandlers()
                            super.onClosed(event)
                        }
                    })
                    if (fuzzierSettingsService.state.resetWindow) {
                        DimensionService.getInstance().setSize(fuzzyDimensionKey, null, project)
                        DimensionService.getInstance().setLocation(fuzzyDimensionKey, null, project)
                        fuzzierSettingsService.state.resetWindow = false
                    }
                    popup!!.showInCenterOf(it)
                    (component as FuzzyFinderComponent).splitPane.dividerLocation =
                        fuzzierSettingsService.state.splitPosition
                }
            }
        }
    }

    override fun updateListContents(project: Project, searchString: String) {
        if (StringUtils.isBlank(searchString)) {
            ApplicationManager.getApplication().invokeLater {
                component.fileList.model = DefaultListModel()
                defaultDoc?.let { (component as FuzzyFinderComponent).previewPane.updateFile(it) }
            }
            return
        }

        currentTask?.takeIf { !it.isDone }?.cancel(true)
        currentTask = ApplicationManager.getApplication().executeOnPooledThread {
            component.fileList.setPaintBusy(true)
            var listModel = DefaultListModel<FuzzyMatchContainer>()

            val stringEvaluator = StringEvaluator(
                fuzzierSettingsService.state.exclusionSet,
                changeListManager
            )

            // Reset modules before creating the content iterator
            val state = service<FuzzierSettingsService>().state
            state.modules = HashMap()
            val moduleManager = ModuleManager.getInstance(project)

            if (moduleManager.modules.size > 1) {
                processModules(moduleManager, state, stringEvaluator, searchString, listModel)
            } else {
                processProject(project, state, stringEvaluator, searchString, listModel)
            }

            listModel = fuzzierUtil.sortAndLimit(listModel)

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

    private fun processModules(moduleManager: ModuleManager, state: FuzzierSettingsService.State,
                               stringEvaluator: StringEvaluator, searchString: String,
                               listModel: DefaultListModel<FuzzyMatchContainer>) {
        for (module in moduleManager.modules) {
            val moduleFileIndex = module.rootManager.fileIndex
            var moduleBasePath = module.rootManager.contentRoots[0].path
            moduleBasePath = moduleBasePath.substringBeforeLast("/")
            state.modules[module.name] = moduleBasePath
            val contentIterator = stringEvaluator.getContentIterator(moduleBasePath, module.name, true, searchString, listModel)
            moduleFileIndex.iterateContent(contentIterator)
        }
    }

    private fun processProject(project: Project, state: FuzzierSettingsService.State, stringEvaluator: StringEvaluator,
                               searchString: String, listModel: DefaultListModel<FuzzyMatchContainer>) {
        val projectFileIndex = ProjectFileIndex.getInstance(project)
        val projectBasePath = project.basePath
        if (projectBasePath != null) {
            state.modules[project.name] = projectBasePath
            val contentIterator = stringEvaluator.getContentIterator(projectBasePath, project.name, false, searchString, listModel)
            projectFileIndex.iterateContent(contentIterator)
        }
    }

    private fun openFile(project: Project, virtualFile: VirtualFile) {
        val fileEditorManager = FileEditorManager.getInstance(project)
        val currentEditor = fileEditorManager.selectedTextEditor
        val previousFile = currentEditor?.virtualFile

        if (fileEditorManager.isFileOpen(virtualFile)) {
            fileEditorManager.openFile(virtualFile, true)
        } else {
            fileEditorManager.openFile(virtualFile, true)
            if (currentEditor != null && !fuzzierSettingsService.state.newTab) {
                fileEditorManager.selectedEditor?.let {
                    if (previousFile != null) {
                        fileEditorManager.closeFile(previousFile)
                    }
                }
            }
        }
        popup?.cancel()
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
                            (component as FuzzyFinderComponent).previewPane.updateFile(file)
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
                        openFile(project, it)
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
                    openFile(project, it)
                }
            }
        })
    }
}