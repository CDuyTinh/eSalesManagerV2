pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "MyeSalesManager"

// One module on purpose, unlike MyeSalesSFA's four. The layers still exist, as
// packages under com.tinhcd.myesalesmanager (core / data / domain / feature /
// navigation); the rule that domain/ imports nothing from android.* or data/ is
// kept by convention instead of by the build.
include(":app")
