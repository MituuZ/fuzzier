import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("org.jetbrains.kotlin.jvm") version "2.1.10"
  id("org.jetbrains.intellij.platform") version "2.3.0"
  id("org.jetbrains.kotlinx.kover") version "0.9.1"
  id("me.champeau.jmh") version "0.7.3"
}

// Use same version and group for the jar and the plugin
val currentVersion = "1.6.0"
val myGroup = "com.mituuz"
version = currentVersion
group = myGroup

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
    intellijIdeaCommunity("2024.3")
    
    pluginVerifier()
    zipSigner()

    testFramework(TestFrameworkType.Platform)
  }
  testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.4")
  testImplementation("org.mockito:mockito-core:5.14.2")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.4")

  // Required to fix issue where JUnit5 Test Framework refers to JUnit4
  // https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-faq.html#junit5-test-framework-refers-to-junit4
  testRuntimeOnly("junit:junit:4.13.2")

  // JMH dependencies
  implementation("org.openjdk.jmh:jmh-core:1.37")
  annotationProcessor("org.openjdk.jmh:jmh-generator-annprocess:1.37")
  jmh("org.jetbrains.kotlin:kotlin-stdlib:2.1.10")
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

intellijPlatform {
  pluginConfiguration {
    version = currentVersion
    group = myGroup
    
    changeNotes = """
    <h2>Version $currentVersion</h2>
    <ul>
      <li>Introduce first version of Fuzzy Grep
        <ul>
          <li>Call <a href="https://github.com/BurntSushi/ripgrep">ripgrep</a> in the background</li>
          <li>Attempt to fall back to <code>grep</code> or <code>findstr</code> if <code>rg</code> is not found</li>
          <li>The following action has been added: <code>map &lt;Leader&gt;ff &lt;action&gt;(com.mituuz.fuzzier.FuzzyGrep)</code></li>
        </ul>
      </li>
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
