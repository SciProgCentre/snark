package space.kscience.snark.pandoc

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
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
    ): Boolean {

        val path = getOrInstallPandoc(pandocExecutablePath)

        try {
            val commandLine = PandocCommandBuilder().apply(commandBuilder).build(path)
            logger.info("Running pandoc: ${commandLine.joinToString(separator = " ")}")
            val pandoc = ProcessBuilder(commandLine).apply {
                if(redirectOutput!= null){
                    redirectOutput(redirectOutput.toFile())
                }
                if(redirectError !=null){
                    redirectError(redirectError.toFile())
                }

            }.start()
            pandoc.waitFor(1, TimeUnit.SECONDS)

            if (pandoc.exitValue() == 0) {
                logger.info("Successfully execute")
                return true
            } else{
                return false
            }
        } catch (e: Exception) {
            logger.error("Got problems with executing: " + e.message)
            return false
        }
    }

}
