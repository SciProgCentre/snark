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
        id("space.kscience.gradle.project") version toolsVersion
        id("space.kscience.gradle.mpp") version toolsVersion
        id("space.kscience.gradle.jvm") version toolsVersion
        id("space.kscience.gradle.js") version toolsVersion
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
            from("space.kscience:version-catalog:$toolsVersion")
        }
    }
}

include(
    ":snark-gradle-plugin",
    ":snark-core",
    ":snark-html",
    ":snark-ktor",
    ":snark-storage-driver",
    ":snark-document-builder",
    ":snark-main",
    ":snark-pandoc-plugin",
)