plugins{
    id("space.kscience.gradle.mpp")
    `maven-publish`
}

val dataforgeVersion: String by rootProject.extra

kscience{
    jvm()
    js()
    dependencies{
        api("space.kscience:dataforge-workspace:$dataforgeVersion")
    }
    useContextReceivers()
}