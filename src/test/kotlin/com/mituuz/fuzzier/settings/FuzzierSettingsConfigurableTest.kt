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
package com.mituuz.fuzzier.settings

import com.intellij.openapi.components.service
import com.intellij.testFramework.TestApplicationManager
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FuzzierSettingsConfigurableTest {
    @Suppress("unused")
    private var testApplicationManager: TestApplicationManager = TestApplicationManager.getInstance()
    private lateinit var state: FuzzierSettingsService.State
    private lateinit var settingsConfigurable: FuzzierSettingsConfigurable
    val factory = IdeaTestFixtureFactory.getFixtureFactory()
    val fixtureBuilder = factory.createFixtureBuilder("Test")
    val fixture = fixtureBuilder.fixture
    val myFixture = IdeaTestFixtureFactory.getFixtureFactory().createCodeInsightFixture(fixture)

    @BeforeEach
    fun setUp() {
        myFixture.setUp()
        val project = myFixture.project
        settingsConfigurable = FuzzierSettingsConfigurable(project)
        state = project.service<FuzzierSettingsService>().state
        state.exclusionSet = setOf("Hello", "There")
    }

    @Test
    fun `Configurable is instanced with no changes`() {
        pre()
        assertFalse(settingsConfigurable.isModified())
    }

    @Test
    fun exclusionSet() {
        pre()
        state.exclusionSet = setOf("Hello", "There", "World")
        assertTrue(settingsConfigurable.isModified())
    }

    @Test
    fun excludedCharacters() {
        pre()
        state.ignoredCharacters = "abc"
        assertTrue(settingsConfigurable.isModified())
    }

    private fun pre() {
        settingsConfigurable.createComponent()
        assertFalse(settingsConfigurable.isModified())
    }
}