import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("space.kscience.gradle.project")
}

allprojects {
    group = "space.kscience"
    version = "0.1.0-dev-1"

    if (name != "snark-gradle-plugin") {
        tasks.withType<KotlinCompile> {
            kotlinOptions {
                freeCompilerArgs = freeCompilerArgs + "-Xcontext-receivers"
            }
        }
    }
}

val dataforgeVersion by extra("0.6.0-dev-15")

ksciencePublish {
    github("SciProgCentre", "snark")
    space("https://maven.pkg.jetbrains.space/mipt-npm/p/sci/maven")
//    sonatype()
}