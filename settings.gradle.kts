rootProject.name = "snark"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {

    val toolsVersion: String by extra

    repositories {
        maven("https://repo.kotlin.link")
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
    }

    plugins {
        id("space.kscience.gradle.project") version toolsVersion
        id("space.kscience.gradle.mpp") version toolsVersion
        id("space.kscience.gradle.jvm") version toolsVersion
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}

dependencyResolutionManagement {

    val toolsVersion: String by extra

    repositories {
        maven("https://repo.kotlin.link")
        mavenLocal()
        mavenCentral()
    }

    versionCatalogs {
        create("spclibs") {
            from("space.kscience:version-catalog:$toolsVersion")
        }
    }
}

include(
    ":snark-core",
    ":snark-html",
    ":snark-ktor",
    ":snark-pandoc",
    ":snark-documents",
//    ":snark-gradle-plugin",
    ":examples:document"
)