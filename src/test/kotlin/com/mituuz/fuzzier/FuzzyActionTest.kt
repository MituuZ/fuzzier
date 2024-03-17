package com.mituuz.fuzzier

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.editor.actionSystem.EditorActionManager
import com.intellij.openapi.project.Project
import com.intellij.testFramework.TestApplicationManager
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.mituuz.fuzzier.StringEvaluator.FilenameType.*
import com.mituuz.fuzzier.StringEvaluator.FuzzyMatchContainer
import com.mituuz.fuzzier.components.SimpleFinderComponent
import com.mituuz.fuzzier.settings.FuzzierSettingsService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.KeyStroke

class FuzzyActionTest {
    private var testApplicationManager: TestApplicationManager
    private val testUtil: TestUtil = TestUtil()
    private var settings: FuzzierSettingsService.State

    init {
        testApplicationManager = TestApplicationManager.getInstance()
        settings = FuzzierSettingsService.State()
    }

    @Test
    fun `Test custom handlers`() {
        val action = getAction()
        val myFixture: CodeInsightTestFixture = testUtil.setUpProject(emptyList())

        action.component = SimpleFinderComponent(myFixture.project)
        action.createSharedListeners(myFixture.project)
        assertNotNull(action.component)

        val inputMap = action.component.searchField.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
        val kShiftKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_K, InputEvent.CTRL_DOWN_MASK)
        val jShiftKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_J, InputEvent.CTRL_DOWN_MASK)

        val moveUpAction = inputMap.get(kShiftKeyStroke)
        val moveDownAction = inputMap.get(jShiftKeyStroke)

        assertEquals("moveUp", moveUpAction)
        assertEquals("moveDown", moveDownAction)

        myFixture.tearDown()
    }

    @Test
    fun `Test custom handlers and reset`() {
        val action = getAction()
        val actionManager = EditorActionManager.getInstance()

        action.setCustomHandlers()
        assertTrue(actionManager.getActionHandler(IdeActions.ACTION_EDITOR_MOVE_CARET_DOWN) is FuzzyAction.FuzzyListActionHandler)
        assertTrue(actionManager.getActionHandler(IdeActions.ACTION_EDITOR_MOVE_CARET_UP) is FuzzyAction.FuzzyListActionHandler)

        action.resetOriginalHandlers()
        assertFalse(actionManager.getActionHandler(IdeActions.ACTION_EDITOR_MOVE_CARET_DOWN) is FuzzyAction.FuzzyListActionHandler)
        assertFalse(actionManager.getActionHandler(IdeActions.ACTION_EDITOR_MOVE_CARET_UP) is FuzzyAction.FuzzyListActionHandler)
    }

    @Test
    fun `Check renderer filename only`() {
        val action = getAction()
        action.setFiletype(FILENAME_ONLY)
        val myFixture: CodeInsightTestFixture = testUtil.setUpProject(emptyList())
        val project = myFixture.project
        action.component = SimpleFinderComponent(project)
        val renderer = action.getCellRenderer()
        val container = FuzzyMatchContainer(1, "/src/asd", "asd")
        val dummyList = JList<FuzzyMatchContainer>()
        val component = renderer.getListCellRendererComponent(dummyList, container, 0, false, false) as JLabel
        assertNotNull(component)
        assertEquals("asd", component.text)
    }

    @Test
    fun `Check renderer with path`() {
        val action = getAction()
        action.setFiletype(FILENAME_WITH_PATH)
        val myFixture: CodeInsightTestFixture = testUtil.setUpProject(emptyList())
        val project = myFixture.project
        action.component = SimpleFinderComponent(project)
        val renderer = action.getCellRenderer()
        val container = FuzzyMatchContainer(1, "/src/asd", "asd")
        val dummyList = JList<FuzzyMatchContainer>()
        val component = renderer.getListCellRendererComponent(dummyList, container, 0, false, false) as JLabel
        assertNotNull(component)
        assertEquals("asd (/src/asd)", component.text)
    }

    @Test
    fun `Check renderer full path`() {
        val action = getAction()
        action.setFiletype(FILEPATH_ONLY)
        val myFixture: CodeInsightTestFixture = testUtil.setUpProject(emptyList())
        val project = myFixture.project
        action.component = SimpleFinderComponent(project)
        val renderer = action.getCellRenderer()
        val container = FuzzyMatchContainer(1, "/src/asd", "asd")
        val dummyList = JList<FuzzyMatchContainer>()
        val component = renderer.getListCellRendererComponent(dummyList, container, 0, false, false) as JLabel
        assertNotNull(component)
        assertEquals("/src/asd", component.text)
    }

    @Test
    fun `Check renderer dir selector`() {
        val action = getAction()
        action.setFiletype(FILENAME_ONLY)
        val myFixture: CodeInsightTestFixture = testUtil.setUpProject(emptyList())
        val project = myFixture.project
        action.component = SimpleFinderComponent(project)
        action.component.isDirSelector = true
        val renderer = action.getCellRenderer()
        val container = FuzzyMatchContainer(1, "/src/asd", "asd")
        val dummyList = JList<FuzzyMatchContainer>()
        val component = renderer.getListCellRendererComponent(dummyList, container, 0, false, false) as JLabel
        assertNotNull(component)
        assertEquals("/src/asd", component.text)
    }

    private fun getAction(): FuzzyAction {
        return object : FuzzyAction() {
            override fun actionPerformed(actionEvent: AnActionEvent) {
            }

            override fun updateListContents(project: Project, searchString: String) {
            }
        }
    }
}