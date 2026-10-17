plugins {
    id("space.kscience.gradle.jvm")
    `kotlin-dsl`
    `java-gradle-plugin`
    `maven-publish`
}

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:${spclibs.versions.kotlin.asProvider().get()}")
    implementation(libs.jsch)
    implementation(projects.snarkKtor)
    implementation("io.ktor:ktor-server-cio")
}

gradlePlugin {
    plugins {
        create("snark-gradle") {
            id = "space.kscience.snark"
            description = "A plugin for snark-based websites"
            implementationClass = "space.kscience.snark.plugin.SnarkGradlePlugin"
        }
    }
}