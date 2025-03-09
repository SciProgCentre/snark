plugins {
    id("space.kscience.gradle.mpp")
    `maven-publish`
}

val dataforgeVersion: String by rootProject.extra
val ktorVersion = space.kscience.gradle.KScienceVersions.ktorVersion

kscience{
    jvm()
    useContextReceivers()

    jvmMain{
        api(projects.snarkHtml)

        api("io.ktor:ktor-server-core:$ktorVersion")
        api("io.ktor:ktor-server-html-builder:$ktorVersion")
        api("io.ktor:ktor-server-host-common:$ktorVersion")
    }
    jvmTest{
        api("io.ktor:ktor-server-tests:$ktorVersion")
    }
}