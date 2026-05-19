plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.1.0"
    id("org.jetbrains.intellij.platform") version "2.3.0"
}

group = "com.papalugo.projectcolorbar"
version = "1.0.8"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        // Build against 2025.1 — compatible with 2024.1 through 2026.x (build 261)
        intellijIdeaCommunity("2025.1")
        instrumentationTools()
        pluginVerifier()
    }
}

intellijPlatform {
    pluginConfiguration {
        name = "Project Color Bar"
        version = "1.0.8"
        description = """
            Color your IDE title bar and borders per project for instant visual identification.
            
            Features:
            - Set a custom color for each project's title bar and window borders
            - Colors are saved per project and restored automatically on open
            - Quick access via Tools menu or right-click on title bar
            - Works on Windows, macOS, and Linux
        """.trimIndent()
        changeNotes = "Initial release"

        ideaVersion {
            sinceBuild = "241"      // IntelliJ 2024.1
            untilBuild = "263.*"    // IntelliJ 2026.3 (covers current IU-261)
        }
    }
    signing {
        // configure if you want to sign the plugin
    }
    publishing {
        // configure if you want to publish to JetBrains Marketplace
    }
    pluginVerification {
        ides {
            recommended()
        }
    }
}

kotlin {
    jvmToolchain(17)
}
