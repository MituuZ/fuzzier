package com.mituuz.fuzzier

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.editor.actionSystem.EditorActionManager
import com.intellij.openapi.project.Project
import com.intellij.testFramework.TestApplicationManager
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.mituuz.fuzzier.components.SimpleFinderComponent
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import javax.swing.JComponent
import javax.swing.KeyStroke

class FuzzyActionTest {
    private var testApplicationManager: TestApplicationManager
    private val testUtil: TestUtil = TestUtil()

    init {
        testApplicationManager = TestApplicationManager.getInstance()
    }

    @Test
    fun `Test custom handlers`() {
        val action = object : FuzzyAction() {
            override fun actionPerformed(actionEvent: AnActionEvent) {
            }

            override fun updateListContents(project: Project, searchString: String) {
            }
        }

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
        val action = object : FuzzyAction() {
            override fun actionPerformed(actionEvent: AnActionEvent) {
            }

            override fun updateListContents(project: Project, searchString: String) {
            }
        }

        val actionManager = EditorActionManager.getInstance()

        action.setCustomHandlers()
        assertTrue(actionManager.getActionHandler(IdeActions.ACTION_EDITOR_MOVE_CARET_DOWN) is FuzzyAction.FuzzyListActionHandler)
        assertTrue(actionManager.getActionHandler(IdeActions.ACTION_EDITOR_MOVE_CARET_UP) is FuzzyAction.FuzzyListActionHandler)

        action.resetOriginalHandlers()
        assertFalse(actionManager.getActionHandler(IdeActions.ACTION_EDITOR_MOVE_CARET_DOWN) is FuzzyAction.FuzzyListActionHandler)
        assertFalse(actionManager.getActionHandler(IdeActions.ACTION_EDITOR_MOVE_CARET_UP) is FuzzyAction.FuzzyListActionHandler)
    }
}