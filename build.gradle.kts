import space.kscience.gradle.useApache2Licence
import space.kscience.gradle.useSPCTeam

plugins {
    id("space.kscience.gradle.project")
}

allprojects {
    group = "space.kscience"
    version = "0.3.0-dev-3"

    repositories {
        maven("https://repo.kotlin.link")
        mavenLocal()
        mavenCentral()
    }

    // dataforge 0.12.0 is compiled for JVM 23; its inline functions can't be inlined into JVM 21 bytecode.
    // Without this, the target follows the JDK that runs Gradle.
    listOf("space.kscience.gradle.mpp", "space.kscience.gradle.jvm").forEach { pluginId ->
        plugins.withId(pluginId) {
            extensions.getByType<space.kscience.gradle.KScienceExtension>().jdkVersionProperty.set(23)
        }
    }
}

kscienceProject {
    pom("https://github.com/SciProgCentre/snark") {
        useApache2Licence()
        useSPCTeam()
    }
    publishTo("spc", "https://maven.sciprog.center/kscience")
    publishToCentral()
}

readme {
    this.useDefaultReadmeTemplate
}