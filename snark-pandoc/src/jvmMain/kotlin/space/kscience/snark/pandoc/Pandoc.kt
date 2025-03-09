package space.kscience.snark.pandoc

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.io.path.Path

public object Pandoc {
    private val logger: Logger = LoggerFactory.getLogger(Pandoc::class.java)

    private fun getOrInstallPandoc(pandocExecutablePath: Path): String = try {
        ProcessBuilder("pandoc", "--version").start().waitFor()
        "pandoc"
    } catch (ex: IOException) {
        if (Files.exists(pandocExecutablePath)) {
            pandocExecutablePath.toAbsolutePath().toString()
        } else {
            logger.info("Pandoc not found in the system. Installing it from GitHub")
            PandocInstaller.installPandoc(pandocExecutablePath).toAbsolutePath().toString()
        }
    }

    /**
     * Call pandoc with options described by commandBuilder.
     * @param commandBuilder
     * @return true if successfully false otherwise
     */
    public fun execute(
        redirectOutput: Path? = null,
        redirectError: Path? = null,
        pandocExecutablePath: Path = Path("./pandoc").toAbsolutePath(),
        commandBuilder: PandocCommandBuilder.() -> Unit,
    ) {

        val path = getOrInstallPandoc(pandocExecutablePath)

        val commandLine = PandocCommandBuilder().apply(commandBuilder).build(path)
        logger.info("Running pandoc: ${commandLine.joinToString(separator = " ")}")
        val pandoc = ProcessBuilder(commandLine).apply {
            if (redirectOutput != null) {
                redirectOutput(redirectOutput.toFile())
            }
            if (redirectError != null) {
                redirectError(redirectError.toFile())
            }

        }.start()
        pandoc.waitFor()

        if (pandoc.exitValue() != 0)
            error("Non-zero process return for pandoc.")
    }
}
