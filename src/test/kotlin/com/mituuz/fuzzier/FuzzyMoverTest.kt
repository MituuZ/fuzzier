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

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.modules
import com.intellij.openapi.project.rootManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiManager
import com.intellij.testFramework.LightVirtualFile
import com.intellij.testFramework.TestApplicationManager
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.mituuz.fuzzier.components.SimpleFinderComponent
import com.mituuz.fuzzier.entities.FuzzyContainer
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
        myFixture.tearDown()
    }

    @Test
    fun `Use the correct steps to move files`() {
        val filePaths = listOf("src/asd/main.log", "src/nope")
        val myFixture: CodeInsightTestFixture = testUtil.setUpProject(filePaths)
        val basePath = myFixture.findFileInTempDir("src").canonicalPath
        val project = myFixture.project

        fuzzyMover.component = SimpleFinderComponent()
        val virtualFile = VirtualFileManager.getInstance().findFileByUrl("file://$basePath/nope")
        val virtualDir = VirtualFileManager.getInstance().findFileByUrl("file://$basePath/asd/")

        fuzzyMover.component.fileList.model = getListModel(virtualFile)
        fuzzyMover.component.fileList.selectedIndex = 0
        if (basePath != null) {
            fuzzyMover.handleInput(project).join()
            fuzzyMover.component.fileList.model = getListModel(virtualDir)
            fuzzyMover.component.fileList.selectedIndex = 0

            fuzzyMover.handleInput(project).thenRun{
                var targetFile = VirtualFileManager.getInstance().findFileByUrl("file://$basePath/asd/nope")
                assertNotNull(targetFile)
                targetFile = VirtualFileManager.getInstance().findFileByUrl("file://$basePath/nope")
                assertNull(targetFile)
            }.join()
        }
        myFixture.tearDown()
    }

    @Test
    fun `Multi module test move to same module`() {
        val module1Files = listOf("src1/main.kt", "src1/test/app.log")
        val module2Files = listOf("src2/tool.kt")
        val myFixture: CodeInsightTestFixture = testUtil.setUpDuoModuleProject(module1Files, module2Files)
        val project = myFixture.project

        assertEquals(2, project.modules.size)

        val basePath = project.modules[0].rootManager.contentRoots[0]

        fuzzyMover.component = SimpleFinderComponent()
        fuzzyMover.currentFile = LightVirtualFile("")
        val virtualFile = VirtualFileManager.getInstance().findFileByUrl("$basePath/main.kt")
        val virtualDir = VirtualFileManager.getInstance().findFileByUrl("$basePath/test/")

        fuzzyMover.component.fileList.model = getListModel(virtualFile)
        fuzzyMover.component.fileList.selectedIndex = 0
        if (basePath != null) {
            fuzzyMover.handleInput(project).join()
            fuzzyMover.component.fileList.model = getListModel(virtualDir)
            fuzzyMover.component.fileList.selectedIndex = 0

            fuzzyMover.handleInput(project).thenRun{
                var targetFile = VirtualFileManager.getInstance().findFileByUrl("$basePath/test/main.kt")
                assertNotNull(targetFile)
                targetFile = VirtualFileManager.getInstance().findFileByUrl("$basePath/main.kt")
                assertNull(targetFile)
            }.join()
        }
        myFixture.tearDown()
    }

    @Test
    fun `Multi module test move to different module`() {
        val module1Files = listOf("src1/MoveMe.kt", "src1/test/app.log")
        val module2Files = listOf("src2/tool.kt", "src2/target/test.kt")
        val myFixture: CodeInsightTestFixture = testUtil.setUpDuoModuleProject(module1Files, module2Files)
        val project = myFixture.project

        assertEquals(2, project.modules.size)

        val module1BasePath = project.modules[0].rootManager.contentRoots[0]
        val module2BasePath = project.modules[1].rootManager.contentRoots[0]

        fuzzyMover.component = SimpleFinderComponent()
        fuzzyMover.currentFile = LightVirtualFile("")
        val virtualFile = VirtualFileManager.getInstance().findFileByUrl("$module1BasePath/MoveMe.kt")
        val virtualDir = VirtualFileManager.getInstance().findFileByUrl("$module2BasePath/target/")

        fuzzyMover.component.fileList.model = getListModel(virtualFile)
        fuzzyMover.component.fileList.selectedIndex = 0
        if (module1BasePath != null) {
            fuzzyMover.handleInput(project).join()
            fuzzyMover.component.fileList.model = getListModel(virtualDir)
            fuzzyMover.component.fileList.selectedIndex = 0

            fuzzyMover.handleInput(project).thenRun{
                var targetFile = VirtualFileManager.getInstance().findFileByUrl("$module2BasePath/target/MoveMe.kt")
                assertNotNull(targetFile)
                targetFile = VirtualFileManager.getInstance().findFileByUrl("$module1BasePath/MoveMe.kt")
                assertNull(targetFile)
            }.join()
        }
        myFixture.tearDown()
    }

    private fun getListModel(virtualFile: VirtualFile?): ListModel<FuzzyContainer?> {
        val listModel = DefaultListModel<FuzzyContainer?>()
        if (virtualFile != null) {
        val container = FuzzyMatchContainer(FuzzyMatchContainer.FuzzyScore(), virtualFile.path, virtualFile.name, "")
            listModel.addElement(container)
        }
        return listModel
    }
}