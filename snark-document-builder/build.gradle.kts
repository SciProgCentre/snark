plugins {
    id("space.kscience.gradle.jvm")
    `maven-publish`
}

val coroutinesVersion = space.kscience.gradle.KScienceVersions.coroutinesVersion
val jacksonVersion = "2.14.2"


dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")

    implementation(project(":snark-storage-driver"))
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
}
