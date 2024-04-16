plugins {
    id("space.kscience.gradle.mpp")
    application
}

application {
    mainClass.set("Mainkt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment", "-Xmx200M")
}

val snarkVersion: String by extra
val ktorVersion = space.kscience.gradle.KScienceVersions.ktorVersion

kscience {
    jvm()
    useContextReceivers()

    jvmMain {
        implementation(projects.snarkKtor)
        implementation("io.ktor:ktor-server-cio:$ktorVersion")
        implementation(spclibs.logback.classic)
    }

    jvmTest{
        implementation("io.ktor:ktor-server-tests:$ktorVersion")
    }
}

kotlin {
    explicitApi = org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode.Disabled
}