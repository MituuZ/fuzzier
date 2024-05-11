package com.mituuz.fuzzier

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiManager
import com.intellij.testFramework.LightVirtualFile
import com.intellij.testFramework.TestApplicationManager
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.mituuz.fuzzier.components.SimpleFinderComponent
import com.mituuz.fuzzier.entities.FuzzyMatchContainer
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import javax.swing.DefaultListModel
import javax.swing.ListModel

class FuzzyMoverTest {
    @Suppress("unused")
    private var testApplicationManager: TestApplicationManager = TestApplicationManager.getInstance()
    private var fuzzyMover: FuzzyMover = FuzzyMover()
    private val testUtil: TestUtil = TestUtil()

    @Test
    fun `Check that files are moved correctly`() {
        val filePaths = listOf("src/asd/main.log", "src/nope")
        val myFixture: CodeInsightTestFixture = testUtil.setUpProject(filePaths)
        val basePath = myFixture.findFileInTempDir("src").canonicalPath
        val project = myFixture.project

        val virtualFile = VirtualFileManager.getInstance().findFileByUrl("file://$basePath/nope")
        val virtualDir = VirtualFileManager.getInstance().findFileByUrl("file://$basePath/asd/")

        fuzzyMover.component = SimpleFinderComponent()
        fuzzyMover.component.fileList.model = getListModel(virtualDir)
        fuzzyMover.component.fileList.selectedIndex = 0
        fuzzyMover.component.isDirSelector = true
        ApplicationManager.getApplication().runReadAction {
            fuzzyMover.movableFile = virtualFile?.let { PsiManager.getInstance(project).findFile(it) }!!
        }
        if (basePath != null) {
            fuzzyMover.handleInput(project).thenRun{
                var targetFile = VirtualFileManager.getInstance().findFileByUrl("file://$basePath/asd/nope")
                assertNotNull(targetFile)
                targetFile = VirtualFileManager.getInstance().findFileByUrl("file://$basePath/nope")
                assertNull(targetFile)
            }.join()
        }
    }

    private fun getListModel(virtualFile: VirtualFile?): ListModel<FuzzyMatchContainer?> {
        val listModel = DefaultListModel<FuzzyMatchContainer?>()
        if (virtualFile != null) {
            val container = FuzzyMatchContainer(FuzzyMatchContainer.FuzzyScore(), virtualFile.path, virtualFile.name)
            listModel.addElement(container)
        }
        return listModel
    }

    @Test
    fun `Use the correct steps to move files`() {
        val filePaths = listOf("src/asd/main.log", "src/nope")
        val myFixture: CodeInsightTestFixture = testUtil.setUpProject(filePaths)
        val basePath = myFixture.findFileInTempDir("src").canonicalPath
        val project = myFixture.project

        fuzzyMover.component = SimpleFinderComponent()
        fuzzyMover.currentFile = LightVirtualFile("/nope")
        if (basePath != null) {
            fuzzyMover.handleInput(project).join()
            fuzzyMover.currentFile = LightVirtualFile("/asd")
            fuzzyMover.handleInput(project).thenRun{
                var targetFile = VirtualFileManager.getInstance().findFileByUrl("file://$basePath/asd/nope")
                assertNotNull(targetFile)
                targetFile = VirtualFileManager.getInstance().findFileByUrl("file://$basePath/nope")
                assertNull(targetFile)
            }.join()
        }
    }
}