/*
MIT License

Copyright (c) 2025 Mitja Leino

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

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.editor.actionSystem.EditorActionManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.testFramework.TestApplicationManager
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.mituuz.fuzzier.components.SimpleFinderComponent
import com.mituuz.fuzzier.entities.FuzzyMatchContainer
import com.mituuz.fuzzier.entities.FuzzyMatchContainer.FuzzyScore
import com.mituuz.fuzzier.entities.FuzzyContainer.FilenameType.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.KeyStroke

class FuzzyActionTest {
    @Suppress("unused")
    private val testApplicationManager: TestApplicationManager = TestApplicationManager.getInstance()
    private val testUtil: TestUtil = TestUtil()

    @Test
    fun `Test custom handlers`() {
        val action = getAction()
        val myFixture: CodeInsightTestFixture = testUtil.setUpProject(emptyList())

        action.component = SimpleFinderComponent()
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
        action.component = SimpleFinderComponent()
        val renderer = action.getCellRenderer()
        val container = FuzzyMatchContainer(FuzzyScore(), "/src/asd", "asd", "")
        val dummyList = JList<FuzzyMatchContainer>()
        val component = renderer.getListCellRendererComponent(dummyList, container, 0, false, false) as JLabel
        assertNotNull(component)
        assertEquals("asd", component.text)
    }

    @Test
    fun `Check renderer with path`() {
        val action = getAction()
        action.setFiletype(FILENAME_WITH_PATH)
        action.component = SimpleFinderComponent()
        val renderer = action.getCellRenderer()
        val container = FuzzyMatchContainer(FuzzyScore(), "/src/asd", "asd", "")
        val dummyList = JList<FuzzyMatchContainer>()
        val component = renderer.getListCellRendererComponent(dummyList, container, 0, false, false) as JLabel
        assertNotNull(component)
        assertEquals("asd   (/src/asd)", component.text)
    }

    @Test
    fun `Check renderer with styled path, no highlight`() {
        val action = getAction()
        action.setFiletype(FILENAME_WITH_PATH_STYLED)
        action.setHighlight(false)
        action.component = SimpleFinderComponent()
        val renderer = action.getCellRenderer()
        val container = FuzzyMatchContainer(FuzzyScore(), "/src/asd", "asd", "")
        val dummyList = JList<FuzzyMatchContainer>()
        val component = renderer.getListCellRendererComponent(dummyList, container, 0, false, false) as JLabel
        assertNotNull(component)
        assertEquals("<html><strong>asd</strong>  <i>(/src/asd)</i></html>", component.text)
    }

    @Test
    fun `Check renderer with styled path, with highlight but no values`() {
        val action = getAction()
        action.setFiletype(FILENAME_WITH_PATH_STYLED)
        action.setHighlight(true)
        action.component = SimpleFinderComponent()
        val renderer = action.getCellRenderer()
        val container = FuzzyMatchContainer(FuzzyScore(), "/src/asd", "asd", "")
        val dummyList = JList<FuzzyMatchContainer>()
        val component = renderer.getListCellRendererComponent(dummyList, container, 0, false, false) as JLabel
        assertNotNull(component)
        assertEquals("<html><strong>asd</strong>  <i>(/src/asd)</i></html>", component.text)
    }

    @Test
    fun `Check renderer full path`() {
        val action = getAction()
        action.setFiletype(FILE_PATH_ONLY)
        action.component = SimpleFinderComponent()
        val renderer = action.getCellRenderer()
        val container = FuzzyMatchContainer(FuzzyScore(), "/src/asd", "asd", "")
        val dummyList = JList<FuzzyMatchContainer>()
        val component = renderer.getListCellRendererComponent(dummyList, container, 0, false, false) as JLabel
        assertNotNull(component)
        assertEquals("/src/asd", component.text)
    }

    @Test
    fun `Check renderer dir selector`() {
        val action = getAction()
        action.setFiletype(FILENAME_ONLY)
        action.component = SimpleFinderComponent()
        action.component.isDirSelector = true
        val renderer = action.getCellRenderer()
        val container = FuzzyMatchContainer(FuzzyScore(), "/src/asd", "asd", "")
        val dummyList = JList<FuzzyMatchContainer>()
        val component = renderer.getListCellRendererComponent(dummyList, container, 0, false, false) as JLabel
        assertNotNull(component)
        assertEquals("/src/asd", component.text)
    }

    private fun getAction(): FuzzyAction {
        return object : FuzzyAction() {
            override fun actionPerformed(actionEvent: AnActionEvent) {
            }

            override fun runAction(project: Project, actionEvent: AnActionEvent) {
            }

            override fun createPopup(screenDimensionKey1: String): JBPopup {
                TODO("Not yet implemented")
            }

            override fun updateListContents(project: Project, searchString: String) {
            }
        }
    }
}