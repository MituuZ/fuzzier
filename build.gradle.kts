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

import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

// Use the same version and group for the jar and the plugin
val currentVersion = "1.8.2"
val myGroup = "com.mituuz"
version = currentVersion
group = myGroup

intellijPlatform {
    pluginConfiguration {
        version = currentVersion
        group = myGroup

        changeNotes = """
    <h2>Version $currentVersion</h2>
    <ul>
      <li>Fix command modification so FuzzyGrepCaseInsensitive now works correctly</li>
      <li>Improve FuzzyGrep string handling and validation for better performance and stability</li>
      <li>Show trimmed row text content in the popup</li>
      <li><strong>Known issue</strong>: command output is handled line by line, but sometimes a single result can 
      span more than one line, resulting in incomplete results. 
      <a href="https://github.com/MituuZ/fuzzier/issues/120">Tracking issue #120</a></li>
    </ul>
    <h2>Version 1.8.1</h2>
    <ul>
      <li>Remove result limit, fix line and column positions for FuzzyGrep</li>
      <li>Update IntelliJ platform plugin to 2.6.0</li>
    </ul>
    """.trimIndent()

        ideaVersion {
            sinceBuild = "243"
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
    alias(libs.plugins.jmh)
}

repositories {
    mavenCentral()

    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        // Downgraded from 2024.3.1.1
        // https://github.com/JetBrains/intellij-platform-gradle-plugin/issues/1838
        intellijIdeaCommunity(libs.versions.communityVersion.get())

        pluginVerifier()
        zipSigner()

        testFramework(TestFrameworkType.Platform)
    }

    // Test dependencies
    testImplementation(libs.mockito)
    testImplementation(libs.junit5Api)
    testImplementation(libs.junit5Engine)

    // Required to fix issue where JUnit5 Test Framework refers to JUnit4
    // https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-faq.html#junit5-test-framework-refers-to-junit4
    testRuntimeOnly(libs.junit4)

    // JMH dependencies
    implementation(libs.jmhCore)
    annotationProcessor(libs.jmhAnnprocessor)
    jmh(libs.kotlinStdlib)
    jmh(fileTree("./libs") { include("*.jar") }) // libs folder contains idea:ideaIC:2024.3 jars
}

tasks.named<Jar>("jmhJar") {
    isZip64 = true
}

tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.koverHtmlReport) // report is always generated after tests run
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

jmh {
    fork = 1
    warmupIterations = 2
    iterations = 2
    timeOnIteration = "2s"
    resultFormat = "JSON"
    jvmArgs = listOf("-Xms2G", "-Xmx2G")
    jmhTimeout = "30s"
    benchmarkMode = listOf("Throughput")
}
