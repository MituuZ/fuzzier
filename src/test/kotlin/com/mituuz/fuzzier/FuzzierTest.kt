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
        service<FuzzierSettingsService>().state.exclusionList = listOf("asd", "nope")
        val filePaths = listOf("src/main.kt", "src/asd/main.kt", "src/asd/asd.kt", "src/not/asd.kt", "src/nope")
        val filePathContainer = setUpProjectFileIndex(filePaths)
        assertEquals(1, filePathContainer.size())
        assertEquals("/main.kt", filePathContainer.get(0).string)
    }

    @Test
    fun excludeListTestNoMatches() {
        service<FuzzierSettingsService>().state.exclusionList = listOf("asd")
        val filePaths = listOf("src/main.kt", "src/not.kt", "src/dsa/not.kt")
        val filePathContainer = setUpProjectFileIndex(filePaths)
        assertEquals(3, filePathContainer.size())
        assertEquals("/main.kt", filePathContainer.get(2).string)
        assertEquals("/not.kt", filePathContainer.get(1).string)
        assertEquals("/dsa/not.kt", filePathContainer.get(0).string)
       }

    @Test
    fun excludeListTestEmptyList() {
        service<FuzzierSettingsService>().state.exclusionList = ArrayList()
        val filePaths = listOf("src/main.kt", "src/not.kt", "src/dsa/not.kt")
        val filePathContainer = setUpProjectFileIndex(filePaths)
        assertEquals(3, filePathContainer.size())
        assertEquals("/main.kt", filePathContainer.get(2).string)
        assertEquals("/not.kt", filePathContainer.get(1).string)
        assertEquals("/dsa/not.kt", filePathContainer.get(0).string)
    }


    private fun setUpProjectFileIndex(filesToAdd: List<String>) : DefaultListModel<Fuzzier.FuzzyMatchContainer> {
        val filePathContainer = DefaultListModel<Fuzzier.FuzzyMatchContainer>()

        val factory = IdeaTestFixtureFactory.getFixtureFactory()
        val fixtureBuilder = factory.createLightFixtureBuilder(null, "Test")
        val fixture = fixtureBuilder.fixture

        val myFixture = IdeaTestFixtureFactory.getFixtureFactory().createCodeInsightFixture(fixture)
        myFixture.setUp()

        filesToAdd.forEach {
            myFixture.addFileToProject(it, "")
        }

        // Add source and wait for indexing
        val dir = myFixture.findFileInTempDir("src")
        val basePath = dir.canonicalPath
        PsiTestUtil.addSourceRoot(fixture.module, dir)
        runInEdtAndWait {
            PsiDocumentManager.getInstance(fixture.project).commitAllDocuments()
        }
        DumbService.getInstance(fixture.project).waitForSmartMode()

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
    fun fuzzyScoreLongSearchString() {
        val match = fuzzier.fuzzyContainsCaseInsensitive("KIF", "TooLongSearchString")
        assertNull(match)
    }

    @Test
    fun fuzzyScoreNoPossibleMatch() {
        val match = fuzzier.fuzzyContainsCaseInsensitive("KIF", "A")
        assertNull(match)
    }

    @Test
    fun fuzzyScoreNoPossibleMatchSplit() {
        val match = fuzzier.fuzzyContainsCaseInsensitive("Kotlin/Is/Fun/kif.kt", "A A B")
        assertNull(match)
    }

    @Test
    fun fuzzyScorePartialMatchSplit() {
        val match = fuzzier.fuzzyContainsCaseInsensitive("Kotlin/Is/Fun/kif.kt", "A A K")
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

        match = fuzzier.fuzzyContainsCaseInsensitive("Kotlin/Is/Fun/kif.kt", "kif fun kotlin")
        assertMatch(40, match)

        match = fuzzier.fuzzyContainsCaseInsensitive("Kotlin/Is/Fun/kif.kt", "is kt")
        assertMatch(22, match)
    }

    @Test
    fun fuzzyScoreSpaceMatch() {
        val match = fuzzier.fuzzyContainsCaseInsensitive("Kotlin/Is/Fun/kif.kt", "fun kotlin")
        assertMatch(29, match)
    }

    private fun assertMatch(score: Int, container: Fuzzier.FuzzyMatchContainer?) {
        if (container != null) {
            assertEquals(score, container.score)
        } else {
            fail("match is null")
        }
    }
}