package com.mituuz.fuzzier

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.testFramework.TestApplicationManager
import com.mituuz.fuzzier.actions.FuzzyAction
import com.mituuz.fuzzier.components.SimpleFinderComponent
import com.mituuz.fuzzier.entities.FuzzyContainer
import com.mituuz.fuzzier.entities.FuzzyMatchContainer
import com.mituuz.fuzzier.entities.FuzzyMatchContainer.FileType.FILE
import com.mituuz.fuzzier.entities.FuzzyMatchContainer.FuzzyScore
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import javax.swing.DefaultListModel

class FuzzyActionRolloverTest {
    @Suppress("unused")
    private val testApplicationManager: TestApplicationManager = TestApplicationManager.getInstance()

    @Test
    fun `Test moveListUp rollover`() {
        val action = getAction()
        action.component = SimpleFinderComponent()
        val model = DefaultListModel<FuzzyContainer>()
        model.addElement(FuzzyMatchContainer(FuzzyScore(), "/src/asd1", "asd1", "", FILE))
        model.addElement(FuzzyMatchContainer(FuzzyScore(), "/src/asd2", "asd2", "", FILE))
        model.addElement(FuzzyMatchContainer(FuzzyScore(), "/src/asd3", "asd3", "", FILE))
        action.component.fileList.model = model
        action.component.fileList.selectedIndex = 0

        action.moveListUp()
        assertEquals(2, action.component.fileList.selectedIndex)

        action.moveListUp()
        assertEquals(1, action.component.fileList.selectedIndex)

        action.moveListUp()
        assertEquals(0, action.component.fileList.selectedIndex)
    }

    @Test
    fun `Test moveListDown rollover`() {
        val action = getAction()
        action.component = SimpleFinderComponent()
        val model = DefaultListModel<FuzzyContainer>()
        model.addElement(FuzzyMatchContainer(FuzzyScore(), "/src/asd1", "asd1", "", FILE))
        model.addElement(FuzzyMatchContainer(FuzzyScore(), "/src/asd2", "asd2", "", FILE))
        model.addElement(FuzzyMatchContainer(FuzzyScore(), "/src/asd3", "asd3", "", FILE))
        action.component.fileList.model = model
        action.component.fileList.selectedIndex = 2

        action.moveListDown()
        assertEquals(0, action.component.fileList.selectedIndex)

        action.moveListDown()
        assertEquals(1, action.component.fileList.selectedIndex)

        action.moveListDown()
        assertEquals(2, action.component.fileList.selectedIndex)
    }

    private fun getAction(): FuzzyAction {
        return object : FuzzyAction() {
            override fun actionPerformed(actionEvent: AnActionEvent) {}
            override fun runAction(project: Project, actionEvent: AnActionEvent) {}
            override fun updateListContents(project: Project, searchString: String) {}
        }
    }
}
