import space.kscience.gradle.*

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

val dataforgeVersion by extra("0.6.2")

ksciencePublish {
    pom("https://github.com/SciProgCentre/snark") {
        useApache2Licence()
        useSPCTeam()
    }
    repository("spc","https://maven.sciprog.center/kscience")
//    sonatype()
}