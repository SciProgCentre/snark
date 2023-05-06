plugins {
    id("space.kscience.gradle.jvm")
    `maven-publish`
}

val coroutinesVersion = space.kscience.gradle.KScienceVersions.coroutinesVersion


dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    implementation(project(":snark-ktor"))
    implementation(project(":snark-storage-driver"))
    implementation(project(":snark-document-builder"))

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.1")
}

tasks.test {
    useJUnitPlatform()
}
