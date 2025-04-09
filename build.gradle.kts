import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

// Use same version and group for the jar and the plugin
val currentVersion = "1.7.1"
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
      <li>Add module info to project settings, unify debug format</li>
    </ul>
    <h2>Version 1.7.0</h2>
    <ul>
      <li>Add new action FuzzyGrepCaseInsensitive with smart case and non-regex search</li>
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
