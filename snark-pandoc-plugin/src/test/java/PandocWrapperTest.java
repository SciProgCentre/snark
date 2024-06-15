import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import space.kscience.snark.pandoc.Installer;
import space.kscience.snark.pandoc.PandocCommandBuilder;
import space.kscience.snark.pandoc.PandocWrapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class PandocWrapperTest {

    private static final Path CORRECT_MD = Path.of("./src/test/testing_directory/first_test.md");
    private static final Path TEX_PATH_TO = Path.of("./src/test/testing_directory/output1.tex");
    private static final Path TESTING_DIRECTORY = Path.of("./src/test/testing_directory");
    private final PandocWrapper pandocWrapper = new PandocWrapper();

    @Test
    public void when_gotPandocAndCorrectArgs_doConverting() {
        try {
            var res = pandocWrapper.use(p -> {
                var command = new PandocCommandBuilder(List.of(CORRECT_MD), TEX_PATH_TO);
                return PandocWrapper.execute(command);
            });
            assertTrue((Boolean) res);
            assertTrue(TEX_PATH_TO.toFile().exists());

            var reader = new BufferedReader(new FileReader(TEX_PATH_TO.toFile()));
            String fileString = reader.lines().collect(Collectors.joining());

            assertTrue(fileString.contains("Some simple text"));
            assertTrue(fileString.contains("\\subsection{Copy elision}"));
            assertTrue(fileString.contains("return"));

            Files.delete(TEX_PATH_TO);

        } catch (Exception ex) {
            fail("Unexpected exception during test when_gotPandocAndCorrectArgs_doConverting()", ex);
        }
    }

    @Test
    public void when_gotPandocAndNotExistsFromFile_then_error() {
        var notExistsFile = Path.of("./src/test/testing_directory/non_exists_test.md");
        assertFalse(notExistsFile.toFile().exists());
        var res = pandocWrapper.use(p -> {
            var command = new PandocCommandBuilder(List.of(notExistsFile), TEX_PATH_TO);
            return PandocWrapper.execute(command);
        });
        assertFalse((Boolean) res);
    }

    @Test
    public void when_gotPandocAndPassDirectory_then_error() {
        assertTrue(TESTING_DIRECTORY.toFile().isDirectory());
        var res = pandocWrapper.use(p -> {
            var command = new PandocCommandBuilder(List.of(TESTING_DIRECTORY), TEX_PATH_TO);
            return PandocWrapper.execute(command);
        });
        assertFalse((Boolean) res);
    }

    @Test
    public void when_askVersionToFile_then_Ok() throws IOException {
        Path outputFile = Files.createTempFile(TESTING_DIRECTORY, "output", ".txt");

        var res = pandocWrapper.use(p -> {
            var command = new PandocCommandBuilder();
            command.getVersion();
            return PandocWrapper.execute(command, outputFile);
        });

        var reader = new BufferedReader(new FileReader(outputFile.toFile()));
        String fileString = reader.lines().collect(Collectors.joining());
        assertTrue(fileString.contains("pandoc"));
        assertTrue(fileString.contains("This is free software"));
        assertTrue((Boolean) res);

        Files.delete(outputFile);
    }

    @Test
    public void when_error_then_writeToErrorStream() throws IOException {
        Path outputFile = Files.createTempFile(TESTING_DIRECTORY, "output", ".txt");
        Path errorFile = Files.createTempFile(TESTING_DIRECTORY, "error", ".txt");

        var res = pandocWrapper.use(p -> {
            var command = new PandocCommandBuilder(List.of(Path.of("./simple.txt")), TEX_PATH_TO);
            command.formatFrom("txt");
            return PandocWrapper.execute(command, outputFile, errorFile);
        });

        var reader = new BufferedReader(new FileReader(errorFile.toFile()));
        String fileString = reader.lines().collect(Collectors.joining());
        assertFalse((Boolean) res);
        assertTrue(fileString.contains("21"));

        Files.delete(outputFile);
        Files.delete(errorFile);
    }



    @Test
    public void when_installPandoc_thenFindIt() {
        Installer.clearInstallingDirectory();
        assertTrue(PandocWrapper.installPandoc());
        assertTrue(PandocWrapper.isPandocInstalled());
    }

    @AfterAll
    public static void clear() {
        Installer.clearInstallingDirectory();
    }
}
