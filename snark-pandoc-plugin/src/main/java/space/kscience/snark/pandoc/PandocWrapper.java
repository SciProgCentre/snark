package space.kscience.snark.pandoc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class PandocWrapper {

    private static final Logger log
            = LoggerFactory.getLogger(PandocWrapper.class);

    private static final Installer installer;

    static {
        try {
            installer = new Installer();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
    private static String pandocPath = "pandoc"; // got pandoc at PATH

    public static String getPandocPath() {
        return pandocPath;
    }

    /**
     * Install pandoc if needed then perform block
     * @param block
     * @return block's return value
     * @param <T>
     */
    public  <T> Object use(Function<PandocWrapper, T> block) {
        if (!isPandocInstalled()) {
            installPandoc();
        }
        return block.apply(this);
    }

    /**
     * Check if pandoc is installed
     * @return true if installed false otherwise
     */
    public static boolean isPandocInstalled() {
        var pb = new PandocCommandBuilder().getVersion();
        return execute(pb);
    }
    /**
     * Call pandoc with options described by commandBuilder.
     * @param commandBuilder
     * @return true if successfully false otherwise
     */
    public static boolean execute(PandocCommandBuilder commandBuilder) {
        return execute(commandBuilder, null, null);
    }

    /**
     * Call pandoc with options described by commandBuilder and log output to outputFile
     * @param commandBuilder
     * @return true if successfully false otherwise
     */
    public static boolean execute(PandocCommandBuilder commandBuilder, Path outputFile) {
        return execute(commandBuilder, outputFile, null);
    }
    /**
     * Call pandoc with options described by commandBuilder and log output to outputFile and error to errorFile.
     * In case errors write exit code to errorFile
     * @param commandBuilder
     * @return true if successfully false otherwise
     */
    public static boolean execute(PandocCommandBuilder commandBuilder, Path outputFile, Path errorFile) {
        try {
            Process pandoc = new ProcessBuilder(commandBuilder.build()).start();
            pandoc.waitFor(1, TimeUnit.SECONDS);

            BufferedReader inp = new BufferedReader(new InputStreamReader(pandoc.getInputStream()));
            String currLine = inp.readLine();

            log.info("log output from pandoc to: {}", outputFile);
            do {
                if (outputFile == null) {
                    log.info(currLine);
                } else {
                    Files.writeString(outputFile, currLine + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                }
            } while ((currLine = inp.readLine()) != null);
            inp.close();

            if (pandoc.exitValue() == 0) {
                log.info("Successfully execute");
                return true;
            } else {
                log.error("Got problems with executing, pandoc exit error: {}", pandoc.exitValue());

                BufferedReader input = new BufferedReader(new InputStreamReader(pandoc.getErrorStream()));
                String line = input.readLine();
                log.info("log error stream from pandoc to: {}", errorFile);

                if (errorFile != null) {
                    Files.writeString(errorFile, "exit code: " + pandoc.exitValue());
                }
                do {
                    if (errorFile == null) {
                        log.info(line);
                    } else {
                        Files.writeString(errorFile, currLine + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    }
                } while ((line = input.readLine()) != null);
                input.close();

                return false;
            }
        } catch (Exception e) {
            log.error("Got problems with executing: " + e.getMessage());
            return false;
        }
    }

    /**
     * Install pandoc and set executable path.
     * @return true if success false otherwise
     */
    public static boolean installPandoc() {
        try {
            pandocPath = installer.installPandoc().toString();
            return true;
        } catch (Exception e) {
            log.error("Got error: {}", e.getMessage(), e);
            return false;
        }
    }

}
