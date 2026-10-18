package space.kscience.snark.plugin

import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.distribution.DistributionContainer
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.named
import space.kscience.dataforge.context.Context
import space.kscience.dataforge.context.request
import space.kscience.dataforge.workspace.readDirectory
import space.kscience.snark.html.SnarkHtml
import space.kscience.snark.ktor.snarkMcp
import java.io.File
import java.time.LocalDateTime

public class SnarkExtension(private val project: Project) {

    private var _dataDirectory: File? = null
    public var dataDirectory: File
        get() = _dataDirectory ?: project.rootDir.resolve(DEFAULT_DATA_PATH)
        set(value) {
            _dataDirectory = value
        }

    public var mcpPort: Int = DEFAULT_MCP_PORT

    public companion object {
        public const val DEFAULT_DATA_PATH: String = "data"
        public const val DEFAULT_MCP_PORT: Int = 3000
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
 *    - Registers task `snarkMcpServer` to run the MCP server.
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

            tasks.register("snarkMcpServer") {
                group = "snark"
                description = "Runs the Snark MCP server."
                doLast {
                    val dataDirectory = snarkExtension.dataDirectory.toPath()
                    logger.info("Reading data for Snark MCP server for data directory: $dataDirectory")
                    val context = Context {
                        plugin(SnarkHtml)
                    }
                    val snark = context.request(SnarkHtml)

                    val data = snark.io.readDirectory(dataDirectory)

                    val server = embeddedServer(CIO, port = snarkExtension.mcpPort) {
                        snarkMcp(context, data)
                    }

                    GlobalScope.launch {
                        val connector = server.application.engine.resolvedConnectors().first()
                        println(
                            "Starting Snark MCP server at http://${connector.host}:${connector.port}"
                        )
                    }
                    val shutdownHook = Thread {
                        server.stop(1000, 2000)
                    }
                    Runtime.getRuntime().addShutdownHook(shutdownHook)
                    try {
                        server.start(wait = true)
                    } finally {
                        try {
                            Runtime.getRuntime().removeShutdownHook(shutdownHook)
                        } catch (_: IllegalStateException) {
                            // JVM is shutting down
                        }
                        server.stop(1000, 2000)
                    }
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