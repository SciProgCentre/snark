plugins{
    id("ru.mipt.npm.gradle.mpp")
    `maven-publish`
}

val dataforgeVersion: String by rootProject.extra

kotlin{
    sourceSets{
        commonMain{
            dependencies{
                api("space.kscience:dataforge-workspace:$dataforgeVersion")
            }
        }
    }
}