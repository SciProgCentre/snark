import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import space.kscience.snark.pandoc.Pandoc
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.readText
import kotlin.io.path.writeBytes
import kotlin.test.assertContains
import kotlin.test.assertFails

class PandocTest {
    @Test
    fun when_gotPandocAndCorrectArgs_doConverting() {

        val inputFile = Files.createTempFile("snark-pandoc", "first_test.md")
        inputFile.writeBytes(javaClass.getResourceAsStream("/first_test.md")!!.readAllBytes())
        val outputFile = Files.createTempFile("snark-pandoc", "output1.tex")

        Pandoc.execute {
            addInputFile(inputFile)
            outputFile(outputFile)
        }

        assertTrue(outputFile.exists())

        val result = outputFile.readText()

        assertContains(result, "Some simple text")
        assertContains(result, "\\subsection{Copy elision}")
        assertContains(result, "return")
    }

    @Test
    fun when_gotPandocAndNotExistsFromFile_then_error() {

        val outputFile = Files.createTempFile("snark-pandoc", "output2.tex")
        val notExistsFile = Path.of("./src/test/testing_directory/non_exists_test.md")
        assertFalse(notExistsFile.exists())
        assertFails {
            Pandoc.execute {
                addInputFile(notExistsFile)
                outputFile(outputFile)
            }
        }
    }

    @Test
    fun when_gotPandocAndPassDirectory_then_error() {
        val tempDir = Files.createTempDirectory("snark-pandoc")
        assertTrue(tempDir.isDirectory())

        val outputFile = Files.createTempFile("snark-pandoc", "output3.tex")

        assertFails {
            Pandoc.execute {
                addInputFile(tempDir)
                outputFile(outputFile)
            }
        }

    }

    @Test
    fun when_askVersionToFile_then_Ok() {
        val outputFile = Files.createTempFile("snark-pandoc", "output4.tex")

        val res = Pandoc.execute(redirectOutput = outputFile) {
            getVersion()
        }

        val fileContent = outputFile.readText()
        assertContains(fileContent, "pandoc")
        assertContains(fileContent, "This is free software")
    }

    @Test
    fun when_error_then_writeToErrorStream() {
        val inputFile = Files.createTempFile("snark-pandoc", "simple.txt")
        inputFile.writeBytes(javaClass.getResourceAsStream("/simple.txt")!!.readAllBytes())
        val outputFile = Files.createTempFile("snark-pandoc", "output.txt")
        val errorFile = Files.createTempFile("snark-pandoc", "error.txt")

        assertFails {
            Pandoc.execute(redirectError = errorFile) {
                addInputFile(inputFile)
                outputFile(outputFile)
                formatFrom("txt")
            }
        }

        assertContains(errorFile.readText(), "input format")
    }


//    @Test
//    fun when_installPandoc_thenFindIt() {
//        PandocInstaller.clearInstallingDirectory()
//        assertTrue(Pandoc.installPandoc())
//        assertTrue(Pandoc.isPandocInstalled())
//    }

}
