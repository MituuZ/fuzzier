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

import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.modules
import com.intellij.openapi.project.rootManager
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.testFramework.PsiTestUtil
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.testFramework.runInEdtAndWait
import com.mituuz.fuzzier.entities.FuzzyContainer
import com.mituuz.fuzzier.entities.StringEvaluator
import org.mockito.ArgumentMatchers.any
import javax.swing.DefaultListModel
import org.mockito.Mockito

class TestUtil {
    private fun addFilesToProject(filesToAdd: List<String>, myFixture: CodeInsightTestFixture, fixture: IdeaProjectTestFixture) {
        if (filesToAdd.isEmpty()) {
            return
        }

        filesToAdd.forEach {
            myFixture.addFileToProject(it, "")
        }

        // Add source and wait for indexing
        val dir = myFixture.findFileInTempDir("src")
        PsiTestUtil.addSourceRoot(fixture.module, dir)
        runInEdtAndWait {
            PsiDocumentManager.getInstance(fixture.project).commitAllDocuments()
        }
        DumbService.getInstance(fixture.project).waitForSmartMode()
    }

    fun setUpModuleFileIndex(filesToAdd: List<String>, exclusionList: Set<String>, ignoredFiles: List<String>? = null) : DefaultListModel<FuzzyContainer> {
        val filePathContainer = DefaultListModel<FuzzyContainer>()
        val factory = IdeaTestFixtureFactory.getFixtureFactory()
        val fixtureBuilder = factory.createLightFixtureBuilder(null, "Test")
        val fixture = fixtureBuilder.fixture
        val myFixture = IdeaTestFixtureFactory.getFixtureFactory().createCodeInsightFixture(fixture)
        val stringEvaluator: StringEvaluator

        myFixture.setUp()
        addFilesToProject(filesToAdd, myFixture, fixture)

        // Create the module map manually, as with test modules the physical path is the second content root
        val map = HashMap<String, String>()
        val module = myFixture.project.modules[0]
        map[module.name] = module.rootManager.contentRoots[1].path

        if (ignoredFiles !== null) {
            val changeListManager = Mockito.mock(ChangeListManager::class.java)
            Mockito.`when`(changeListManager.isIgnoredFile(any<VirtualFile>())).thenAnswer { invocation ->
                val file = invocation.getArgument<VirtualFile>(0)
                val tempDirPath = myFixture.tempDirPath
                ignoredFiles.any{ ("$tempDirPath/$it") == file.path }
            }
            stringEvaluator = StringEvaluator(exclusionList, map, changeListManager)
        } else {
            stringEvaluator = StringEvaluator(exclusionList, map)
        }

        val contentIterator = stringEvaluator.getContentIterator(myFixture.module.name, "", filePathContainer, null)
        val index = myFixture.module.rootManager.fileIndex
        runInEdtAndWait {
            index.iterateContent(contentIterator)
        }
        // Handle clearing ProjectFileIndex between tests
        myFixture.tearDown()

        return filePathContainer
    }

    fun setUpProject(filesToAdd: List<String>): CodeInsightTestFixture {
        val factory = IdeaTestFixtureFactory.getFixtureFactory()
        val fixtureBuilder = factory.createLightFixtureBuilder(null, "Test")
        val fixture = fixtureBuilder.fixture
        val myFixture = IdeaTestFixtureFactory.getFixtureFactory().createCodeInsightFixture(fixture)

        myFixture.setUp()
        addFilesToProject(filesToAdd, myFixture, fixture)
        return myFixture
    }

    fun setUpMultiModuleProject(vararg moduleList: List<String>): CodeInsightTestFixture {
        val factory = IdeaTestFixtureFactory.getFixtureFactory()
        val fixtureBuilder = factory.createFixtureBuilder("Test")
        val fixture = fixtureBuilder.fixture
        val myFixture = IdeaTestFixtureFactory.getFixtureFactory().createCodeInsightFixture(fixture)
        myFixture.setUp()
        val project = myFixture.project

        for (moduleFiles in moduleList) {
            addFiles(moduleFiles.drop(1), myFixture)

            val modulePath = myFixture.findFileInTempDir(moduleFiles[0])
            val module = WriteAction.computeAndWait<Module, RuntimeException> {
                ModuleManager.getInstance(project).newModule(modulePath.path, "Empty")
            }
            PsiTestUtil.addSourceRoot(module, modulePath)
        }

        runInEdtAndWait {
            PsiDocumentManager.getInstance(fixture.project).commitAllDocuments()
        }
        DumbService.getInstance(fixture.project).waitForSmartMode()

        return myFixture
    }
    
    fun setUpDuoModuleProject(module1Files: List<String>, module2Files: List<String>, customModule2Path: String = "src2"): CodeInsightTestFixture {
        val module2List: MutableList<String> = ArrayList()
        module2List.add(customModule2Path)
        module2List.addAll(module2Files)

        val module1List: MutableList<String> = ArrayList()
        module1List.add("src1")
        module1List.addAll(module1Files)

        return setUpMultiModuleProject(module1List, module2List)
    }

    private fun addFiles(files: List<String>, myFixture: CodeInsightTestFixture) {
        for (file in files) {
            myFixture.addFileToProject(file, "")
        }
    }
}