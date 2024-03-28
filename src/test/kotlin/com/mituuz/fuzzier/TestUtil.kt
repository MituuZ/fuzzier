package com.mituuz.fuzzier

import com.intellij.openapi.project.DumbService
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.psi.PsiDocumentManager
import com.intellij.testFramework.PsiTestUtil
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.testFramework.runInEdtAndWait
import com.mituuz.fuzzier.StringEvaluator.FuzzyMatchContainer
import javax.swing.DefaultListModel

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

    fun setUpProjectFileIndex(filesToAdd: List<String>, exclusionList: Set<String>, ignoredFiles: List<String>? = null) : DefaultListModel<FuzzyMatchContainer> {
        val filePathContainer = DefaultListModel<FuzzyMatchContainer>()
        val factory = IdeaTestFixtureFactory.getFixtureFactory()
        val fixtureBuilder = factory.createLightFixtureBuilder(null, "Test")
        val fixture = fixtureBuilder.fixture
        val myFixture = IdeaTestFixtureFactory.getFixtureFactory().createCodeInsightFixture(fixture)
        val stringEvaluator: StringEvaluator

        myFixture.setUp()
        addFilesToProject(filesToAdd, myFixture, fixture)

        if (ignoredFiles !== null) {
            val changeListManager = ChangeListManager.getInstance(fixture.project)
            /* some way to set ignored files on this changeListManager */
            stringEvaluator = StringEvaluator(true, exclusionList, 5, 10, 10, changeListManager)
        } else {
            stringEvaluator = StringEvaluator(true, exclusionList, 5, 10, 10)
        }

        val basePath = myFixture.findFileInTempDir("src").canonicalPath
        val contentIterator = basePath?.let { stringEvaluator.getContentIterator(it, "", filePathContainer) }
        val index = ProjectFileIndex.getInstance(fixture.project)
        runInEdtAndWait {
            if (contentIterator != null) {
                index.iterateContent(contentIterator)
            }
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
}