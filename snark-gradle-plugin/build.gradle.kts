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
    implementation(npmlibs.kotlin.gradle)
    implementation("com.github.mwiede:jsch:0.2.1")
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