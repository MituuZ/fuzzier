package com.mituuz.fuzzier

import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.psi.PsiDocumentManager
import com.intellij.testFramework.PsiTestUtil
import com.intellij.testFramework.TestApplicationManager
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.testFramework.runInEdtAndWait
import com.mituuz.fuzzier.settings.FuzzierSettingsService
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import javax.swing.DefaultListModel

class ExcludeTest {
    private var fuzzier: Fuzzier
    private var testApplicationManager: TestApplicationManager

    init {
        testApplicationManager = TestApplicationManager.getInstance()
        fuzzier = Fuzzier()
    }

    @Test
    fun excludeListTest() {
        service<FuzzierSettingsService>().state.exclusionList = listOf("asd", "nope")
        val filePaths = listOf("src/main.kt", "src/asd/main.kt", "src/asd/asd.kt", "src/not/asd.kt", "src/nope")
        val filePathContainer = setUpProjectFileIndex(filePaths)
        Assertions.assertEquals(1, filePathContainer.size())
        Assertions.assertEquals("/main.kt", filePathContainer.get(0).string)
    }

    @Test
    fun excludeListTestNoMatches() {
        service<FuzzierSettingsService>().state.exclusionList = listOf("asd")
        val filePaths = listOf("src/main.kt", "src/not.kt", "src/dsa/not.kt")
        val filePathContainer = setUpProjectFileIndex(filePaths)
        Assertions.assertEquals(3, filePathContainer.size())
        Assertions.assertEquals("/main.kt", filePathContainer.get(2).string)
        Assertions.assertEquals("/not.kt", filePathContainer.get(1).string)
        Assertions.assertEquals("/dsa/not.kt", filePathContainer.get(0).string)
    }

    @Test
    fun excludeListTestEmptyList() {
        service<FuzzierSettingsService>().state.exclusionList = ArrayList()
        val filePaths = listOf("src/main.kt", "src/not.kt", "src/dsa/not.kt")
        val filePathContainer = setUpProjectFileIndex(filePaths)
        Assertions.assertEquals(3, filePathContainer.size())
        Assertions.assertEquals("/main.kt", filePathContainer.get(2).string)
        Assertions.assertEquals("/not.kt", filePathContainer.get(1).string)
        Assertions.assertEquals("/dsa/not.kt", filePathContainer.get(0).string)
    }

    @Test
    fun excludeListTestStartsWith() {
        service<FuzzierSettingsService>().state.exclusionList = listOf("/asd*")
        val filePaths = listOf("src/main.kt", "src/asd/main.kt", "src/asd/asd.kt", "src/not/asd.kt")
        val filePathContainer = setUpProjectFileIndex(filePaths)
        Assertions.assertEquals(2, filePathContainer.size())
        Assertions.assertEquals("/not/asd.kt", filePathContainer.get(0).string)
    }

    @Test
    fun excludeListTestEndsWith() {
        service<FuzzierSettingsService>().state.exclusionList = listOf("*.log")
        val filePaths = listOf("src/main.log", "src/asd/main.log", "src/asd/asd.kt", "src/not/asd.kt", "src/nope")
        val filePathContainer = setUpProjectFileIndex(filePaths)
        Assertions.assertEquals(3, filePathContainer.size())
        Assertions.assertEquals("/asd/asd.kt", filePathContainer.get(0).string)
    }

    private fun setUpProjectFileIndex(filesToAdd: List<String>) : DefaultListModel<Fuzzier.FuzzyMatchContainer> {
        val filePathContainer = DefaultListModel<Fuzzier.FuzzyMatchContainer>()
        val factory = IdeaTestFixtureFactory.getFixtureFactory()
        val fixtureBuilder = factory.createLightFixtureBuilder(null, "Test")
        val fixture = fixtureBuilder.fixture
        val myFixture = IdeaTestFixtureFactory.getFixtureFactory().createCodeInsightFixture(fixture)

        myFixture.setUp()
        addFilesToProject(filesToAdd, myFixture, fixture)

        val basePath = myFixture.findFileInTempDir("src").canonicalPath
        val contentIterator = basePath?.let { fuzzier.getContentIterator(it, "", filePathContainer) }
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

    private fun addFilesToProject(filesToAdd: List<String>, myFixture: CodeInsightTestFixture, fixture: IdeaProjectTestFixture) {
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
}