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
}
