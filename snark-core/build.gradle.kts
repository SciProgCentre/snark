plugins {
    id("space.kscience.gradle.mpp")
    `maven-publish`
}

kscience {
    jvm()
    js()

    commonMain {
        api(libs.dataforge.io)
        api(libs.dataforge.workspace)
    }

    jsMain {
        api(project.dependencies.platform(spclibs.kotlin.js.wrappers))
    }
}