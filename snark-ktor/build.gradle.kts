plugins {
    id("space.kscience.gradle.mpp")
    `maven-publish`
}


kscience{
    jvm()
    useContextParameters()

    commonMain{
        api(project.dependencies.platform(spclibs.ktor.bom))
    }

    jvmMain{
        api(projects.snarkHtml)

        api("io.ktor:ktor-server-core")
        api("io.ktor:ktor-server-html-builder")
        api("io.ktor:ktor-server-host-common")
    }
    jvmTest{
        api("io.ktor:ktor-server-test-host")
    }
}