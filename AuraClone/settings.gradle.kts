pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
// dependencyResolutionManagement removed to use allprojects in root build.gradle.kts for better stability
rootProject.name = "AuraClone"
include(":app")
