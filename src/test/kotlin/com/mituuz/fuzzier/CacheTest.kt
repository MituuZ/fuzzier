package com.mituuz.fuzzier

import com.intellij.openapi.components.service
import com.intellij.testFramework.TestApplicationManager
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.mituuz.fuzzier.settings.FuzzierSettingsService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import javax.swing.DefaultListModel

class CacheTest {
    private var fuzzier: Fuzzier
    private var testApplicationManager: TestApplicationManager
    private lateinit var filePathContainer: DefaultListModel<Fuzzier.FuzzyMatchContainer>
    private lateinit var fixture: IdeaProjectTestFixture
    private lateinit var myFixture: CodeInsightTestFixture
    private var testUtil = TestUtil()

    init {
        testApplicationManager = TestApplicationManager.getInstance()
        fuzzier = Fuzzier()
    }

    @BeforeEach
    fun setUp() {
        service<FuzzierSettingsService>().state.exclusionList = listOf()
        filePathContainer = DefaultListModel<Fuzzier.FuzzyMatchContainer>()
        val factory = IdeaTestFixtureFactory.getFixtureFactory()
        val fixtureBuilder = factory.createLightFixtureBuilder(null, "Test")
        fixture = fixtureBuilder.fixture
        myFixture = IdeaTestFixtureFactory.getFixtureFactory().createCodeInsightFixture(fixture)
        myFixture.setUp()
    }

    @AfterEach
    fun tearDown() {
        myFixture.tearDown()
    }

    @Test
    fun `Basic functionality test for the cache`() {
        val filePaths = listOf("src/main.kt", "src/asd/main.kt", "src/asd/asd.kt", "src/not/asd.kt", "src/nope")
        testUtil.addFilesToProject(filePaths, myFixture, fixture)

        val projectBasePath = myFixture.findFileInTempDir("src").canonicalPath
        if (projectBasePath != null) {
            var searchString = "a"
            fuzzier.processFiles(searchString, fixture.project, projectBasePath, filePathContainer)
            Assertions.assertEquals(4, fuzzier.filePathCacheTemp.size)
            Assertions.assertFalse(fuzzier.usedCache)

            searchString = "as"
            fuzzier.processFiles(searchString, fixture.project, projectBasePath, filePathContainer)
            Assertions.assertEquals(3, fuzzier.filePathCacheTemp.size)
            Assertions.assertTrue(fuzzier.usedCache)

            searchString = "asd"
            fuzzier.processFiles(searchString, fixture.project, projectBasePath, filePathContainer)
            Assertions.assertEquals(3, fuzzier.filePathCacheTemp.size)
            Assertions.assertTrue(fuzzier.usedCache)

            searchString = "as"
            fuzzier.processFiles(searchString, fixture.project, projectBasePath, filePathContainer)
            Assertions.assertEquals(3, fuzzier.filePathCacheTemp.size)
            Assertions.assertFalse(fuzzier.usedCache)
        }
    }

    @Test
    fun `Test cache reset when changing the search string`() {
        val filePaths = listOf("src/main.kt", "src/asd/main.kt", "src/asd/asd.kt", "src/not/asd.kt", "src/nope")
        testUtil.addFilesToProject(filePaths, myFixture, fixture)

        val projectBasePath = myFixture.findFileInTempDir("src").canonicalPath
        if (projectBasePath != null) {
            var searchString = "a"
            fuzzier.processFiles(searchString, fixture.project, projectBasePath, filePathContainer)
            Assertions.assertEquals(4, fuzzier.filePathCacheTemp.size)
            Assertions.assertFalse(fuzzier.usedCache)

            searchString = "as"
            fuzzier.processFiles(searchString, fixture.project, projectBasePath, filePathContainer)
            Assertions.assertEquals(3, fuzzier.filePathCacheTemp.size)
            Assertions.assertTrue(fuzzier.usedCache)

            searchString = "n"
            fuzzier.processFiles(searchString, fixture.project, projectBasePath, filePathContainer)
            Assertions.assertEquals(4, fuzzier.filePathCacheTemp.size)
            Assertions.assertFalse(fuzzier.usedCache)
        }
    }

    @Test
    fun `Test cache functionality when changing string and no results found`() {
        val filePaths = listOf("src/main.kt", "src/asd/main.kt", "src/asd/asd.kt", "src/not/asd.kt", "src/nope")
        testUtil.addFilesToProject(filePaths, myFixture, fixture)

        val projectBasePath = myFixture.findFileInTempDir("src").canonicalPath
        if (projectBasePath != null) {
            var searchString = "a"
            fuzzier.processFiles(searchString, fixture.project, projectBasePath, filePathContainer)
            Assertions.assertEquals(4, fuzzier.filePathCacheTemp.size)
            Assertions.assertFalse(fuzzier.usedCache)

            searchString = "as"
            fuzzier.processFiles(searchString, fixture.project, projectBasePath, filePathContainer)
            Assertions.assertEquals(3, fuzzier.filePathCacheTemp.size)
            Assertions.assertTrue(fuzzier.usedCache)

            searchString = "b"
            fuzzier.processFiles(searchString, fixture.project, projectBasePath, filePathContainer)
            Assertions.assertEquals(0, fuzzier.filePathCacheTemp.size)
            Assertions.assertFalse(fuzzier.usedCache)
        }
    }
}