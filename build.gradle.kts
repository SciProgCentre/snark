plugins {
    id("space.kscience.gradle.project")
}

allprojects {
    group = "space.kscience"
    version = "0.1.0-dev-1"

    repositories {
        mavenCentral()
        mavenLocal()
    }
}

val dataforgeVersion by extra("0.6.1-dev-6")

ksciencePublish {
    github("SciProgCentre", "snark")
    space("https://maven.pkg.jetbrains.space/mipt-npm/p/sci/maven")
//    sonatype()
}