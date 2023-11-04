package space.kscience.snark.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.FileTree
import org.gradle.kotlin.dsl.withType
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
                val outputFile = project.layout.buildDirectory.file("resources/main/buildDate")
                doLast {
                    val deployDate = LocalDateTime.now()
                    outputFile.get().asFile.run {
                        parentFile.mkdirs()
                        writeText(deployDate.toString())
                    }
                }
                outputs.file(outputFile)
                outputs.upToDateWhen { false }
            }

            tasks.getByName("processResources").dependsOn(writeBuildDate)

            extensions.configure<org.gradle.api.tasks.SourceSetContainer>("sourceSets") {
                getByName("main") {
                    logger.info("Adding ${snarkExtension.dataDirectory} to resources")
                    resources.srcDir(snarkExtension.dataDirectory)
                }
            }
        }

        tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
            kotlinOptions {
                freeCompilerArgs = freeCompilerArgs + "-Xcontext-receivers"
            }
        }
    }
}