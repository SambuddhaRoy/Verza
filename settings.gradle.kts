pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // NewPipeExtractor is built from a commit by JitPack.
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "Verza"
include(":app")
