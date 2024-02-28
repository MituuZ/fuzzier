package com.mituuz.fuzzier

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiManager
import com.intellij.testFramework.TestApplicationManager
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture
import com.mituuz.fuzzier.components.SimpleFinderComponent
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import javax.swing.SwingUtilities

class FuzzyMoverTest {
    private var fuzzyMover: FuzzyMover
    private var testApplicationManager: TestApplicationManager
    private val testUtil: TestUtil = TestUtil()

    init {
        testApplicationManager = TestApplicationManager.getInstance()
        fuzzyMover = FuzzyMover()
    }

    @Test
    fun `Check that files are moved correctly`() {
        val filePaths = listOf("src/asd/main.log", "src/nope")
        val myFixture: CodeInsightTestFixture = testUtil.setUpProject(filePaths)
        val basePath = myFixture.findFileInTempDir("src").canonicalPath
        val project = myFixture.project

        val virtualFile = VirtualFileManager.getInstance().findFileByUrl("file://$basePath/nope")

        fuzzyMover.component = SimpleFinderComponent(project)
        fuzzyMover.component.isDirSelector = true
        fuzzyMover.currentFile = "/asd"
        ApplicationManager.getApplication().runReadAction {
            fuzzyMover.movableFile = virtualFile?.let { PsiManager.getInstance(project).findFile(it) }!!
        }
        if (basePath != null) {
            fuzzyMover.handleInput(basePath, project).thenRun{
                var targetFile = VirtualFileManager.getInstance().findFileByUrl("file://$basePath/asd/nope")
                assertNotNull(targetFile)
                targetFile = VirtualFileManager.getInstance().findFileByUrl("file://$basePath/nope")
                assertNull(targetFile)
            }.join()
        }
    }

    @Test
    fun `Use the correct steps to move files`() {
        val filePaths = listOf("src/asd/main.log", "src/nope")
        val myFixture: CodeInsightTestFixture = testUtil.setUpProject(filePaths)
        val basePath = myFixture.findFileInTempDir("src").canonicalPath
        val project = myFixture.project

        fuzzyMover.component = SimpleFinderComponent(project)
        fuzzyMover.currentFile = "/nope"
        if (basePath != null) {
            fuzzyMover.handleInput(basePath, project).join()
            fuzzyMover.currentFile = "/asd"
            fuzzyMover.handleInput(basePath, project).thenRun{
                var targetFile = VirtualFileManager.getInstance().findFileByUrl("file://$basePath/asd/nope")
                assertNotNull(targetFile)
                targetFile = VirtualFileManager.getInstance().findFileByUrl("file://$basePath/nope")
                assertNull(targetFile)
            }.join()
        }
    }
}