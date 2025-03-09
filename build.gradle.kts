import space.kscience.gradle.useApache2Licence
import space.kscience.gradle.useSPCTeam

plugins {
    id("space.kscience.gradle.project")
}

allprojects {
    group = "space.kscience"
    version = "0.2.0-dev-1"

    repositories {
        mavenCentral()
        mavenLocal()
    }
}

val dataforgeVersion by extra("0.8.0")

ksciencePublish {
    pom("https://github.com/SciProgCentre/snark") {
        useApache2Licence()
        useSPCTeam()
    }
    repository("spc", "https://maven.sciprog.center/kscience")
//    sonatype()
}

readme {
    this.useDefaultReadmeTemplate
}