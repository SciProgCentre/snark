plugins {
    id("ru.mipt.npm.gradle.project")
}

allprojects {
    group = "space.kscience"
    version = "0.1.0-dev-1"

    if(name!="snark-gradle-plugin") {
        tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
            kotlinOptions {
                freeCompilerArgs = freeCompilerArgs  + "-Xcontext-receivers"
            }
        }
    }
}

val dataforgeVersion by extra("0.6.0-dev-10")