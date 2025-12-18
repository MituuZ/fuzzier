/*
 *  MIT License
 *
 *  Copyright (c) 2025 Mitja Leino
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

// Use the same version and group for the jar and the plugin
val currentVersion = "2.0.0-pre1"
val myGroup = "com.mituuz"
version = currentVersion
group = myGroup

intellijPlatform {
    pluginConfiguration {
        version = currentVersion
        group = myGroup

        changeNotes = """
    <h2>Version $currentVersion</h2>
    <p>This version contains larger refactors and multiple new actions enabled by them.</p>
    <p>I'm updating the existing package structure to keep things nicer and not supporting the old actions to avoid possible problems in the future.</p>

    <h3>Breaking changes</h3>
    <p><strong>Rename existing actions</strong></p>
    <ul>
        <li><code>com.mituuz.fuzzier.FuzzyGrepCaseInsensitive</code> to <code>com.mituuz.fuzzier.grep.FuzzyGrepCI</code></li>
        <li><code>com.mituuz.fuzzier.FuzzyGrep</code> to <code>com.mituuz.fuzzier.grep.FuzzyGrep</code></li>
        <li><code>com.mituuz.fuzzier.Fuzzier</code> to <code>com.mituuz.fuzzier.search.Fuzzier</code></li>
        <li><code>com.mituuz.fuzzier.FuzzierVCS</code> to <code>com.mituuz.fuzzier.search.FuzzierVCS</code></li>
        <li><code>com.mituuz.fuzzier.FuzzyMover</code> to <code>com.mituuz.fuzzier.operation.FuzzyMover</code></li>
    </ul>
    <p><strong>Update default list movement keys</strong></p>
    <ul>
        <li>From <code>CTRL + j</code> and <code>CTRL + k</code> to <code>CTRL + n</code> and <code>CTRL + p</code></li>
    </ul>

    <h3>New actions</h3>
    <p>Added some new grep variations</p>
    <ul>
        <li><code>com.mituuz.fuzzier.grep.FuzzyGrepOpenTabsCI</code></li>
        <li><code>com.mituuz.fuzzier.grep.FuzzyGrepOpenTabs</code></li>
        <li><code>com.mituuz.fuzzier.grep.FuzzyGrepCurrentBufferCI</code></li>
        <li><code>com.mituuz.fuzzier.grep.FuzzyGrepCurrentBuffer</code></li>
    </ul>

    <h3>New features</h3>
    <ul>
        <li>Popup now defaults to auto-sized, which scales with the current window</li>
        <li>You can revert this from the settings</li>
    </ul>

    <h3>Other changes</h3>
    <ul>
        <li>Refactor file search to use coroutines</li>
        <li>Handle list size limiting during processing instead of doing them separately</li>
        <li>Add debouncing for file preview using <code>SingleAlarm</code></li>
        <li>Refactor everything</li>
        <li>Remove manual handling of the divider location (use JBSplitter instead) and unify styling</li>
    </ul>
    
    """.trimIndent()

        ideaVersion {
            sinceBuild = "251"
            untilBuild = provider { null }
        }
    }

    publishing {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }

    signing {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }
}

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.intellijPlatform)
    alias(libs.plugins.kover)
}

repositories {
    mavenCentral()

    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        // TODO: Update minimum version to 2025.3 and replace this deprecated config
        intellijIdeaCommunity(libs.versions.communityVersion.get())
        pluginVerifier()
        zipSigner()
        testFramework(TestFrameworkType.Platform)
    }

    testImplementation(libs.junit5Api)
    testImplementation(libs.junit5Engine)
    testImplementation(libs.mockk)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // Required to fix an issue where JUnit5 Test Framework refers to JUnit4
    // https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-faq.html#junit5-test-framework-refers-to-junit4
    testRuntimeOnly(libs.junit4)
}

// mockk brings its own coroutines version (1.10.1), so exclude it from the classpath
// and use the one provided by the platform (1.8.0)
// TODO: Check later if the coroutine versions can be aligned
configurations.matching { it.name in listOf("testImplementation", "testCompileClasspath") }
    .all {
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core")
    }

tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.koverHtmlReport)
}

kover {
    reports {
        filters {
            excludes {
                classes("com.mituuz.fuzzier.performance.*")
            }
        }
    }
}


tasks.withType<KotlinCompile> {
    compilerOptions.jvmTarget.set(JvmTarget.JVM_21)
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
}
