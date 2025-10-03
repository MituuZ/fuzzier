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
val currentVersion = "1.11.0"
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
      <li>Update deprecated method calls</li>
      <li>Update dependencies</li>
      <li>Remove JMH deps</li>
      <li>Migrate from mockito to mockk</li>
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

// mockk brings its own coroutines version, so exclude it from the classpath
configurations.matching { it.name in listOf("implementation", "compileClasspath", "runtimeClasspath") }
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
