plugins {
    id("space.kscience.gradle.mpp")
    `maven-publish`
}


kscience{
    jvm()

    commonMain{
        api(project.dependencies.platform(spclibs.ktor.bom))
    }

    jvmMain{
        api(projects.snarkHtml)

        implementation(libs.mcp.server)

        api("io.ktor:ktor-server-core")
        api("io.ktor:ktor-server-html-builder")
        api("io.ktor:ktor-server-host-common")
    }
    jvmTest{
        api("io.ktor:ktor-server-test-host")
    }
}