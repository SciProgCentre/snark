import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import space.kscience.snark.pandoc.Pandoc
import java.io.BufferedReader
import java.io.FileReader
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Collectors
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.div

class PandocTest {
    @Test
    fun when_gotPandocAndCorrectArgs_doConverting() {
        try {
            val res = Pandoc.execute {
                addInputFile(CORRECT_MD)
                outputFile(TEX_PATH_TO)
            }
            assertTrue(res)
            assertTrue(TEX_PATH_TO.toFile().exists())

            val reader = BufferedReader(FileReader(TEX_PATH_TO.toFile()))
            val fileString = reader.lines().collect(Collectors.joining())

            assertTrue(fileString.contains("Some simple text"))
            assertTrue(fileString.contains("\\subsection{Copy elision}"))
            assertTrue(fileString.contains("return"))

            Files.delete(TEX_PATH_TO)
        } catch (ex: Exception) {
            fail<Any>("Unexpected exception during test when_gotPandocAndCorrectArgs_doConverting()", ex)
        }
    }

    @Test
    fun when_gotPandocAndNotExistsFromFile_then_error() {
        val notExistsFile = Path.of("./src/test/testing_directory/non_exists_test.md")
        assertFalse(notExistsFile.toFile().exists())
        val res = Pandoc.execute {
            addInputFile(notExistsFile)
            outputFile(TEX_PATH_TO)
        }
        assertFalse(res)
    }

    @Test
    fun when_gotPandocAndPassDirectory_then_error() {
        assertTrue(TESTING_DIRECTORY.toFile().isDirectory)

        val res = Pandoc.execute {
            addInputFile(TESTING_DIRECTORY)
            outputFile(TEX_PATH_TO)
        }

        assertFalse(res)
    }

    @Test
    fun when_askVersionToFile_then_Ok() {
        val outputFile = TESTING_DIRECTORY/"output.txt"

        val res = Pandoc.execute(redirectOutput = outputFile) {
            getVersion()
        }

        val reader = BufferedReader(FileReader(outputFile.toFile()))
        val fileString = reader.lines().collect(Collectors.joining())
        assertTrue(fileString.contains("pandoc"))
        assertTrue(fileString.contains("This is free software"))
        assertTrue(res)
    }

    @Test
    fun when_error_then_writeToErrorStream() {
        val outputFile = Files.createTempFile(TESTING_DIRECTORY, "output", ".txt")
        val errorFile = Files.createTempFile(TESTING_DIRECTORY, "error", ".txt")

        val res = Pandoc.execute(outputFile, errorFile) {
            addInputFile(Path.of("./simple.txt"))
            outputFile(TEX_PATH_TO)
            formatFrom("txt")
        }

        val reader = BufferedReader(FileReader(errorFile.toFile()))
        val fileString = reader.lines().collect(Collectors.joining())
        assertFalse(res)
        assertTrue(fileString.contains("21"))

        Files.delete(outputFile)
        Files.delete(errorFile)
    }


//    @Test
//    fun when_installPandoc_thenFindIt() {
//        PandocInstaller.clearInstallingDirectory()
//        assertTrue(Pandoc.installPandoc())
//        assertTrue(Pandoc.isPandocInstalled())
//    }

    companion object {
        private val TESTING_DIRECTORY: Path = Path("./testing_directory").apply {
            createDirectories()
        }
        private val CORRECT_MD: Path = TESTING_DIRECTORY.resolve("first_test.md")
        private val TEX_PATH_TO: Path = TESTING_DIRECTORY.resolve("output1.tex")
    }
}
