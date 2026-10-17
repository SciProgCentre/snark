plugins {
    id("space.kscience.gradle.mpp")
    `maven-publish`
}

kscience {
    jvm()
    useSerialization()
    commonMain {
        api(projects.snarkHtml)
    }

}