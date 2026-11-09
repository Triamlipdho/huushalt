// Maven Central often answers CI with "429 Too Many Requests". Google's official mirror of Maven
// Central is tried first; Maven Central itself stays as the fallback.
val mavenCentralMirror = "https://maven-central.storage-download.googleapis.com/maven2/"

pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        maven("https://maven-central.storage-download.googleapis.com/maven2/") { name = "MavenCentralMirror" }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        maven(mavenCentralMirror) { name = "MavenCentralMirror" }
        mavenCentral()
    }
}

rootProject.name = "huushalt"
include(":app")
