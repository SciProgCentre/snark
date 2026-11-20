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