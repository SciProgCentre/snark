plugins {
    id("space.kscience.gradle.jvm")
    `maven-publish`
}

val dataforgeVersion: String by rootProject.extra
val ktorVersion = space.kscience.gradle.KScienceVersions.ktorVersion

kscience{
    useContextReceivers()
}

dependencies {
    api(projects.snarkHtml)

    api("io.ktor:ktor-server-core:$ktorVersion")
    api("io.ktor:ktor-server-html-builder:$ktorVersion")
    api("io.ktor:ktor-server-host-common:$ktorVersion")

    testApi("io.ktor:ktor-server-tests:$ktorVersion")
}