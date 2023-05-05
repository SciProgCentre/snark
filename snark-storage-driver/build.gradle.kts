plugins {
    id("space.kscience.gradle.jvm")
    `maven-publish`
}

val coroutinesVersion = space.kscience.gradle.KScienceVersions.coroutinesVersion
val awsSdkVersion = "0.+"


dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")

    // s3 Driver dependency
    implementation("aws.sdk.kotlin:s3:$awsSdkVersion")

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.1")
}

tasks.test {
    useJUnitPlatform()
}
