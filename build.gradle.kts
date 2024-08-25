import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("org.jetbrains.kotlin.jvm") version "2.0.20"
  id("org.jetbrains.intellij.platform") version "2.0.1"
}

// Use same version and group for the jar and the plugin
val currentVersion = "1.0.0"
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
    intellijIdeaCommunity("2024.2.0.1")
    
    pluginVerifier()
    zipSigner()
    instrumentationTools()
    
    testFramework(TestFrameworkType.Platform)
  }
  
  testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.2")
  testImplementation("org.mockito:mockito-core:5.12.0")

  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.2")
  
  // Required to fix issue where JUnit5 Test Framework refers to JUnit4
  // https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-faq.html#junit5-test-framework-refers-to-junit4
  testRuntimeOnly("junit:junit:4.13.2")
}

tasks.test {
  useJUnitPlatform()
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
    - Re-implement project file handling as a backup if no modules are present<br>
    - Migrate IntelliJ Platform Gradle Plugin to 2.x
    """.trimIndent()
    
    ideaVersion {
      sinceBuild = "242"
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