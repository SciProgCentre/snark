plugins {
    id("ru.mipt.npm.gradle.jvm")
}

val dataforgeVersion: String by rootProject.extra
val ktorVersion = ru.mipt.npm.gradle.KScienceVersions.ktorVersion

dependencies {
    api(projects.snarkHtml)

    api("io.ktor:ktor-server-core:$ktorVersion")
    api("io.ktor:ktor-server-html-builder:$ktorVersion")
    api("io.ktor:ktor-server-host-common:$ktorVersion")

    testApi("io.ktor:ktor-server-tests:$ktorVersion")
}