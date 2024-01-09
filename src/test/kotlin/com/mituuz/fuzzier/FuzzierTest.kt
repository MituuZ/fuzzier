package com.mituuz.fuzzier

import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.psi.PsiDocumentManager
import com.intellij.testFramework.PsiTestUtil
import com.intellij.testFramework.TestApplicationManager
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.testFramework.runInEdtAndWait
import com.mituuz.fuzzier.settings.FuzzierSettingsService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import javax.swing.DefaultListModel

class FuzzierTest {
    private var fuzzier: Fuzzier
    private var testApplicationManager: TestApplicationManager

    init {
        testApplicationManager = TestApplicationManager.getInstance()
        fuzzier = Fuzzier()
    }

    @Test
    fun excludeListTest() {
        val filePathContainer = DefaultListModel<Fuzzier.FuzzyMatchContainer>()
        val list = ArrayList<String>()
        list.add("ASD")
        val projectBasePath = "basepath"
        val searchString = "b"

        val contentIterator = fuzzier.getContentIterator(projectBasePath, searchString, filePathContainer)

        service<FuzzierSettingsService>().state.exclusionList = list

        val factory = IdeaTestFixtureFactory.getFixtureFactory()
        val fixtureBuilder = factory.createLightFixtureBuilder(null, "Test")
        val fixture = fixtureBuilder.fixture

        val myFixture = IdeaTestFixtureFactory.getFixtureFactory().createCodeInsightFixture(fixture)
        myFixture.setUp()

        myFixture.addFileToProject("src/main.kt", "file content")
        myFixture.addFileToProject("main.kt", "file content")


        val dir = myFixture.findFileInTempDir("src/main.kt")
        println("File 1: " + myFixture.findFileInTempDir("src/main.kt"))
        println("File 2: " + myFixture.findFileInTempDir("src"))

        PsiTestUtil.addSourceRoot(fixture.module, dir)

        runInEdtAndWait {
            PsiDocumentManager.getInstance(fixture.project).commitAllDocuments()
        }

        DumbService.getInstance(fixture.project).waitForSmartMode()

        val index = ProjectFileIndex.getInstance(fixture.project)
        runInEdtAndWait {
            index.iterateContent(contentIterator)
        }
    }

    @Test
    fun fuzzyScoreEmptyString() {
        val match = fuzzier.fuzzyContainsCaseInsensitive("", "")
        assertMatch(0, match)
    }

    @Test
    fun fuzzyScoreNoStreak() {
        val match = fuzzier.fuzzyContainsCaseInsensitive("KotlinIsFun", "kif")
        assertMatch(1, match)
    }

    @Test
    fun fuzzyScoreStreak() {
        val match = fuzzier.fuzzyContainsCaseInsensitive("KotlinIsFun", "kot")
        assertMatch(3, match)
    }

    @Test
    fun fuzzyScoreNoPossibleMatch() {
        val match = fuzzier.fuzzyContainsCaseInsensitive("KIF", "TooLongSearchString")
        assertNull(match)
    }

    @Test
    fun fuzzyScoreFilePathMatch() {
        var match = fuzzier.fuzzyContainsCaseInsensitive("Kotlin/Is/Fun/kif.kt", "kif")
        assertMatch(11, match)

        match = fuzzier.fuzzyContainsCaseInsensitive("Kiffer/Is/Fun/kif.kt", "kif")
        assertMatch(13, match)

        match = fuzzier.fuzzyContainsCaseInsensitive("Kiffer/Is/Fun/kiffer.kt", "kif")
        assertMatch(3, match)

        match = fuzzier.fuzzyContainsCaseInsensitive("Kotlin/Is/Fun/kif.kt", "kt")
        assertMatch(11, match)

        match = fuzzier.fuzzyContainsCaseInsensitive("Kif/Is/Fun/kif.kt", "kif")
        assertMatch(23, match)
    }

    private fun assertMatch(score: Int, container: Fuzzier.FuzzyMatchContainer?) {
        if (container != null) {
            assertEquals(score, container.score)
        } else {
            fail("match is null")
        }
    }
}