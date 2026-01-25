plugins {
    id("space.kscience.gradle.mpp")
//    alias(spclibs.plugins.ktor)
}

kscience {
    jvm{
        binaries {
            executable {
                mainClass.set("center.sciprog.snark.documents.MainKt")

                val isDevelopment: Boolean = ext.has("development")
                applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment", "-Xmx200M")
            }
        }
    }

    useContextParameters()

    jvmMain {
        implementation(projects.snarkKtor)
        implementation("io.ktor:ktor-server-cio")
        implementation(spclibs.logback.classic)
    }

    jvmTest {
        implementation("io.ktor:ktor-server-test-host")
    }
}

kotlin {
    explicitApi = org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode.Disabled
}