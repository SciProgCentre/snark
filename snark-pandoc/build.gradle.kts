plugins {
    id("space.kscience.gradle.mpp")
}


kscience {
    useSerialization {
        json()
    }
    jvm()
    jvmMain {
        api(spclibs.slf4j)
        implementation("org.apache.commons:commons-exec:1.3")
        implementation("org.apache.commons:commons-compress:1.2")
    }
    jvmTest{
        implementation(spclibs.logback.classic)
    }

}
