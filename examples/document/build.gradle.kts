plugins {
    id("space.kscience.gradle.mpp")
    alias(spclibs.plugins.ktor)
}

kscience {
    jvm{
        withJava()
    }
    useContextReceivers()
    useKtor()

    jvmMain {
        implementation(projects.snarkKtor)
        implementation("io.ktor:ktor-server-cio")
        implementation(spclibs.logback.classic)
    }

    jvmTest {
        implementation("io.ktor:ktor-server-tests")
    }
}

kotlin {
    explicitApi = org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode.Disabled
}

application {
    mainClass.set("center.sciprog.snark.documents.MainKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment", "-Xmx200M")
}