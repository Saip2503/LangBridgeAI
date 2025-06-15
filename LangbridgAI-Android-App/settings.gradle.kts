// settings.gradle.kts
pluginManagement {
    repositories {
        gradlePluginPortal() // For Gradle plugins themselves
        google() // For Google's Android plugins and libraries
        mavenCentral() // For other common libraries and plugins
    }
}

// This block ensures that the dependency resolution order for plugins is defined.
// It is often combined with pluginManagement if repositories are shared.
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "LangbridgAI-Android-App" // This is the name of your root project

// Include your 'app' module. The path should match your module's folder name.
include(":app")
