rootProject.name = "snark"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
enableFeaturePreview("VERSION_CATALOGS")

pluginManagement {

    val toolsVersion: String by extra

    repositories {
        maven("https://repo.kotlin.link")
        mavenCentral()
        gradlePluginPortal()
    }

    plugins {
        id("ru.mipt.npm.gradle.project") version toolsVersion
        id("ru.mipt.npm.gradle.mpp") version toolsVersion
        id("ru.mipt.npm.gradle.jvm") version toolsVersion
        id("ru.mipt.npm.gradle.js") version toolsVersion
    }
}

dependencyResolutionManagement {

    val toolsVersion: String by extra

    repositories {
        maven("https://repo.kotlin.link")
        mavenCentral()
    }

    versionCatalogs {
        create("npmlibs") {
            from("ru.mipt.npm:version-catalog:$toolsVersion")
        }
    }
}

include(
    ":snark-gradle-plugin",
    ":snark-core",
    ":snark-html",
    ":snark-ktor"
)