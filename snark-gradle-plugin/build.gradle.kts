plugins{
    id("space.kscience.gradle.jvm")
    `kotlin-dsl`
    `java-gradle-plugin`
    `maven-publish`
}

repositories{
    gradlePluginPortal()
}

dependencies{
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:${spclibs.versions.kotlin.asProvider().get()}")
    implementation("com.github.mwiede:jsch:0.2.26")
}

gradlePlugin{
    plugins {
        create("snark-gradle") {
            id = "space.kscience.snark"
            description = "A plugin for snark-based websites"
            implementationClass = "space.kscience.snark.plugin.SnarkGradlePlugin"
        }
    }
}