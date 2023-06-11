plugins {
    id("space.kscience.gradle.jvm")
    `maven-publish`
}

val dataforgeVersion: String by rootProject.extra
val ktorVersion = space.kscience.gradle.KScienceVersions.ktorVersion

dependencies {
    api(projects.snarkHtml)

    api("io.ktor:ktor-server-core:$ktorVersion")
    api("io.ktor:ktor-server-html-builder:$ktorVersion")
    api("io.ktor:ktor-server-host-common:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:2.3.0")
    implementation(project(":snark-storage-driver"))
    implementation("io.ktor:ktor-server-partial-content:$ktorVersion")
    implementation("io.ktor:ktor-server-auto-head-response:$ktorVersion")

    testApi("io.ktor:ktor-server-tests:$ktorVersion")
}