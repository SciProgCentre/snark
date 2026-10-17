plugins {
    id("space.kscience.gradle.mpp")
    `maven-publish`
}

val dataforgeVersion: String by rootProject.extra

kscience {
    jvm()
    js()
    dependencies {
        api(libs.dataforge.workspace)
    }

    jsMain {
        api(project.dependencies.platform("org.jetbrains.kotlin-wrappers:kotlin-wrappers-bom:2026.1.0"))
    }
}