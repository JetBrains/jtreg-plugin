import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

fun properties(key: String) = providers.gradleProperty(key)

plugins {
    id("java")
    kotlin("plugin.serialization") version "2.2.20"
    id("org.jetbrains.kotlin.jvm") version "2.2.20"
    id("org.jetbrains.intellij") version "1.17.4"
    id("org.jetbrains.changelog") version "2.1.2"
}

group = "com.jetbrains"
version = properties("pluginVersion").get()

val javaHarnessLib = "lib" + File.separator + "javatest.jar"

repositories {
    mavenCentral()
}

dependencies {
    api(files(javaHarnessLib))
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:1.9.25")
    testImplementation("org.mockito:mockito-core:5.15.2")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")
}


intellij {
    version.set(providers.gradleProperty("ideaVersion").orElse("2024.1.6"))
    type.set("IC")

    plugins.set(listOf("java", "TestNG-J"))
}

changelog {
    groups.empty()
    repositoryUrl = properties("pluginRepositoryUrl")
}

tasks {

    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)  // or JvmTarget.JVM_21
        }
    }

    // Allow setting the jtreg vendor in resources via -PjtregVendor=jetbrains|openjdk
    processResources {
        val vendor = (project.findProperty("jtregVendor") as String?)?.lowercase() ?: "openjdk"
        // Ensure stable inputs for caching
        inputs.property("jtregVendor", vendor)
        filesMatching("jtreg.vendor") {
            expand(mapOf("jtregVendor" to vendor))
            filteringCharset = "UTF-8"
        }
    }

    patchPluginXml {
        version = properties("pluginVersion")
        sinceBuild.set("241")
        untilBuild.set("261.0")

        pluginDescription = providers.fileContents(layout.projectDirectory.file("README.md")).asText.map {
            val start = "<!-- Plugin description -->"
            val end = "<!-- Plugin description end -->"

            with(it.lines()) {
                if (!containsAll(listOf(start, end))) {
                    throw GradleException("Plugin description section not found in README.md:\n$start ... $end")
                }
                subList(indexOf(start) + 1, indexOf(end)).joinToString("\n").let(::markdownToHTML)
            }
        }

        val changelog = project.changelog // local variable for configuration cache compatibility
        // Get the latest available change notes from the changelog file
        changeNotes = version.map { pluginVersion ->
            with(changelog) {
                renderItem(
                    (getOrNull(pluginVersion) ?: getUnreleased())
                        .withHeader(false)
                        .withEmptySections(false),
                    Changelog.OutputType.HTML,
                )
            }
        }
    }

    publishPlugin {
        dependsOn("patchChangelog")
    }

//    signPlugin {
//        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
//        privateKey.set(System.getenv("PRIVATE_KEY"))
//        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
//    }
//
//    publishPlugin {
//        token.set(System.getenv("PUBLISH_TOKEN"))
//    }
}
