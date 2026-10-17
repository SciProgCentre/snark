package space.kscience.snark.plugin

import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.distribution.DistributionContainer
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.named
import space.kscience.snark.plugin.mcp.SnarkMcpExtension
import java.io.File
import java.time.LocalDateTime

public class SnarkExtension(private val project: Project) {

    private var _dataDirectory: File? = null
    public var dataDirectory: File
        get() = _dataDirectory ?: project.rootDir.resolve(DEFAULT_DATA_PATH)
        set(value) {
            _dataDirectory = value
        }

    public val mcp: SnarkMcpExtension = SnarkMcpExtension()

    public fun mcp(action: Action<SnarkMcpExtension>) {
        action.execute(mcp)
    }

    public fun mcp(action: SnarkMcpExtension.() -> Unit) {
        mcp.action()
    }

    public companion object {
        public const val DEFAULT_DATA_PATH: String = "data"
    }
}

/**
 * A custom Gradle plugin that introduces additional functionality for projects.
 * This plugin automatically applies configuration for specific Gradle plugins and introduces a custom extension `snark`.
 *
 * When applied to a project, this plugin:
 *
 * 1. Creates and adds a custom extension `snark` of type `SnarkExtension` to the project.
 *    The extension can be used to configure properties, such as the default data directory.
 *
 * 2. If the `org.jetbrains.kotlin.jvm` plugin is applied:
 *    - Registers a task named `writeBuildDate` which generates a file containing the current build date and time.
 *    - Configures the `processResources` task to depend on the `writeBuildDate` task, ensuring the build date is written
 *      during the build process.
 *    - Registers tasks `snarkMcpServer` (STDIO transport) and `snarkMcpServerDaemon` (HTTP transport) to run the MCP server.
 *
 * 3. If the `org.gradle.application` plugin is applied:
 *    - Configures the main application distribution to include the contents of the `dataDirectory` from the `snark` extension
 *      under the `data` subdirectory in the distribution layout.
 *
 * This plugin is designed to streamline the management of build-time metadata and data file distribution for Gradle-based projects.
 */
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

            tasks.register("snarkMcpServer", JavaExec::class.java) {
                group = "snark"
                description = "Starts the Snark MCP server on STDIO."
                mainClass.set("space.kscience.snark.ktor.mcp.SnarkMcpServer")
                val sourceSets = project.extensions.findByType(SourceSetContainer::class.java)
                val mainSourceSet = sourceSets?.findByName("main")
                if (mainSourceSet != null) {
                    classpath = mainSourceSet.runtimeClasspath
                }
                standardInput = System.`in`
                doFirst {
                    val dataDir = snarkExtension.mcp.dataDirectory ?: snarkExtension.dataDirectory
                    args = listOf(
                        "--data", dataDir.absolutePath,
                        "--transport", snarkExtension.mcp.transport.name,
                        "--port", snarkExtension.mcp.port.toString(),
                        "--watch", snarkExtension.mcp.watch.toString()
                    )
                }
            }

            tasks.register("snarkMcpServerDaemon", JavaExec::class.java) {
                group = "snark"
                description = "Runs the Snark MCP server in background HTTP mode."
                mainClass.set("space.kscience.snark.ktor.mcp.SnarkMcpServer")
                val sourceSets = project.extensions.findByType(SourceSetContainer::class.java)
                val mainSourceSet = sourceSets?.findByName("main")
                if (mainSourceSet != null) {
                    classpath = mainSourceSet.runtimeClasspath
                }
                doFirst {
                    val dataDir = snarkExtension.mcp.dataDirectory ?: snarkExtension.dataDirectory
                    args = listOf(
                        "--data", dataDir.absolutePath,
                        "--transport", "HTTP",
                        "--port", snarkExtension.mcp.port.toString(),
                        "--watch", snarkExtension.mcp.watch.toString()
                    )
                }
            }
        }

        plugins.withId("org.gradle.application") {
            extensions.findByType<DistributionContainer>()?.apply {
                named<org.gradle.api.distribution.Distribution>("main") {
                    contents {
                        from(snarkExtension.dataDirectory) {
                            into("data")
                        }
                    }
                }
            }
        }

    }
}