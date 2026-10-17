plugins {
    id("space.kscience.gradle.mpp")
    `maven-publish`
}

kscience {
    jvm()
    js()

    commonMain {
        api(libs.dataforge.workspace)
    }

    jsMain {
        api(project.dependencies.platform("org.jetbrains.kotlin-wrappers:kotlin-wrappers-bom:2026.1.0"))
    }
}