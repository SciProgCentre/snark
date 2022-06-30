package space.kscience.snark.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import java.io.File
import java.time.LocalDateTime

public class SnarkExtension(private val project: Project) {

    private var _dataDirectory: File? = null
    public var dataDirectory: File
        get() = _dataDirectory ?: project.rootDir.resolve(DEFAULT_DATA_PATH)
        set(value) {
            _dataDirectory = value
        }

    public companion object {
        public const val DEFAULT_DATA_PATH: String = "data"
    }
}


public class SnarkGradlePlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        val snarkExtension = SnarkExtension(this)
        extensions.add("snark", snarkExtension)

        plugins.withId("org.jetbrains.kotlin.jvm") {
            val writeBuildDate = tasks.register("writeBuildDate") {
                val outputFile = File(project.buildDir, "resources/main/buildDate")
                doLast {
                    val deployDate = LocalDateTime.now()
                    outputFile.parentFile.mkdirs()
                    outputFile.writeText(deployDate.toString())
                }
                outputs.file(outputFile)
                outputs.upToDateWhen { false }
            }

            tasks.getByName("processResources").dependsOn(writeBuildDate)

            configure<KotlinJvmProjectExtension> {
                sourceSets.apply {
                    getByName("main") {
                        resources.srcDir(project.rootDir.resolve("data"))
                    }
                }
            }
        }
    }
}