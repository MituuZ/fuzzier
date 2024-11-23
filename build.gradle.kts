import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("org.jetbrains.kotlin.jvm") version "2.1.0-RC"
  id("org.jetbrains.intellij.platform") version "2.1.0"
  id("org.jetbrains.kotlinx.kover") version "0.9.0-RC"
}

// Use same version and group for the jar and the plugin
val currentVersion = "1.3.0"
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
    intellijIdeaCommunity("2024.2.1")
    
    pluginVerifier()
    zipSigner()
    instrumentationTools()
    
    testFramework(TestFrameworkType.Platform)
  }
  
  testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.3")
  testImplementation("org.mockito:mockito-core:5.14.2")

  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.3")
  
  // Required to fix issue where JUnit5 Test Framework refers to JUnit4
  // https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-faq.html#junit5-test-framework-refers-to-junit4
  testRuntimeOnly("junit:junit:4.13.2")
}

tasks.test {
  useJUnitPlatform()
  finalizedBy(tasks.koverHtmlReport) // report is always generated after tests run
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
    - Add option to list recently searched files on popup open<br>
    - Update some dependencies
    """.trimIndent()
    
    ideaVersion {
      sinceBuild = "242"
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
