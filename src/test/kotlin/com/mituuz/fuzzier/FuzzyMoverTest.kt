package com.mituuz.fuzzier

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiManager
import com.intellij.testFramework.TestApplicationManager
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.mituuz.fuzzier.StringEvaluator.FuzzyMatchContainer
import com.mituuz.fuzzier.components.SimpleFinderComponent
import com.mituuz.fuzzier.settings.FuzzierSettingsService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.SwingUtilities

class FuzzyMoverTest {
    private var fuzzyMover: FuzzyMover
    private var testApplicationManager: TestApplicationManager
    private val testUtil: TestUtil = TestUtil()
    private var settings: FuzzierSettingsService.State

    init {
        testApplicationManager = TestApplicationManager.getInstance()
        fuzzyMover = FuzzyMover()
        settings  = service<FuzzierSettingsService>().state
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

    @Test
    fun `Check renderer filename only`() {
        settings.filenameType = StringEvaluator.FilenameType.FILENAME_ONLY
        val myFixture: CodeInsightTestFixture = testUtil.setUpProject(emptyList())
        val project = myFixture.project
        fuzzyMover.component = SimpleFinderComponent(project)
        val renderer = fuzzyMover.getCellRenderer()
        val container = FuzzyMatchContainer(1, "/src/asd", "asd")
        val dummyList = JList<FuzzyMatchContainer>()
        val component = renderer.getListCellRendererComponent(dummyList, container, 0, false, false) as JLabel
        assertNotNull(component)
        assertEquals("asd", component.text)
    }

    @Test
    fun `Check renderer with path`() {
        settings.filenameType = StringEvaluator.FilenameType.FILENAME_WITH_PATH
        val myFixture: CodeInsightTestFixture = testUtil.setUpProject(emptyList())
        val project = myFixture.project
        fuzzyMover.component = SimpleFinderComponent(project)
        val renderer = fuzzyMover.getCellRenderer()
        val container = FuzzyMatchContainer(1, "/src/asd", "asd")
        val dummyList = JList<FuzzyMatchContainer>()
        val component = renderer.getListCellRendererComponent(dummyList, container, 0, false, false) as JLabel
        assertNotNull(component)
        assertEquals("asd (/src/asd)", component.text)
    }

    @Test
    fun `Check renderer full path`() {
        settings.filenameType = StringEvaluator.FilenameType.FILEPATH_ONLY
        val myFixture: CodeInsightTestFixture = testUtil.setUpProject(emptyList())
        val project = myFixture.project
        fuzzyMover.component = SimpleFinderComponent(project)
        val renderer = fuzzyMover.getCellRenderer()
        val container = FuzzyMatchContainer(1, "/src/asd", "asd")
        val dummyList = JList<FuzzyMatchContainer>()
        val component = renderer.getListCellRendererComponent(dummyList, container, 0, false, false) as JLabel
        assertNotNull(component)
        assertEquals("/src/asd", component.text)
    }

    @Test
    fun `Check renderer dir selector`() {
        settings.filenameType = StringEvaluator.FilenameType.FILENAME_ONLY
        val myFixture: CodeInsightTestFixture = testUtil.setUpProject(emptyList())
        val project = myFixture.project
        fuzzyMover.component = SimpleFinderComponent(project)
        fuzzyMover.component.isDirSelector = true
        val renderer = fuzzyMover.getCellRenderer()
        val container = FuzzyMatchContainer(1, "/src/asd", "asd")
        val dummyList = JList<FuzzyMatchContainer>()
        val component = renderer.getListCellRendererComponent(dummyList, container, 0, false, false) as JLabel
        assertNotNull(component)
        assertEquals("/src/asd", component.text)
    }
}