package space.kscience.snark.pandoc;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.exec.OS;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Installer {

    private static final Logger log
            = LoggerFactory.getLogger(Installer.class);

    public enum OSType {
        WINDOWS("windows-x86_64.zip", "windows"),
        MAC_OS_AMD("x86_64-macOS.zip", "mac.os.amd"),
        MAC_OS_ARM("arm64-macOS.zip", "mac.os.arm"),
        LINUX_ARM("linux-arm64", "linux.arm"),
        LINUX_AMD("linux-amd64", "linux.amd");

        private final String assetSuffix;
        private final String propertySuffix;
        OSType(String assetSuf, String propertySuf) {
            assetSuffix = assetSuf;
            propertySuffix = propertySuf;
        }
        public String getAssetSuffix() {
            return assetSuffix;
        }
        public String getPropertySuffix() {
            return propertySuffix;
        }
    }

    private static class OSData {
        private URL urlForInstalling;
        private Path fileToInstall;
        private Path pathToPandoc;
        public OSData() {}
        public void setUrlForInstalling(URL urlForInstalling) {
            this.urlForInstalling = urlForInstalling;
        }
        public URL getUrlForInstalling() {
            return urlForInstalling;
        }
        public void setFileToInstall(Path fileToInstall) {
            this.fileToInstall = fileToInstall;
        }
        public Path getFileToInstall() {
            return fileToInstall;
        }
        public Path getPathToPandoc() {
            return pathToPandoc;
        }
        public void setPathToPandoc(Path pathToPandoc) {
            this.pathToPandoc = pathToPandoc;
        }
    }

    private static Map<OSType, OSData> dataForInstalling = new HashMap<>(OSType.values().length);
    private static final Properties properties = new Properties();
    private static Path pandocDir = Path.of("./pandoc").toAbsolutePath();
    private static final int TIMEOUT_SECONDS = 2;
    private static final int ATTEMPTS = 3;

    Installer() throws IOException, InterruptedException {
        try {
            properties.load(new FileInputStream(
                    Thread.currentThread().getContextClassLoader().getResource("installer.properties").getPath()
            ));
        } catch (Exception ex) {
            log.error("Error during download properties, ex: {}", ex.getMessage(), ex);
            throw ex;
        }

        initFiles();
        var resp = getGithubUrls();
        initUrls(resp);
    }

    private void initFiles() {
        for (var os : OSType.values()) {
            dataForInstalling.put(os, new OSData());
            switch (os) {
                case LINUX_AMD :
                case LINUX_ARM :
                        dataForInstalling.get(os)
                                .setFileToInstall(Path.of(pandocDir.toString() + "/pandoc.tar.gz"));
                        break;
                default :
                        dataForInstalling.get(os)
                                .setFileToInstall(Path.of(pandocDir.toString() + "/pandoc.zip"));
            }
        }
    }

    private void initUrls(ResponseDto responseDto) throws IOException {

        for (var os : OSType.values()) {
            var asset = responseDto.getAssetByOsSuffix(os.getAssetSuffix());
            var currUrl = asset.getBrowserDownloadUrl();

            var currPath = properties.getProperty("path.to.pandoc." + os.getPropertySuffix()).replace("{version}",
                    responseDto.getTagName());

            dataForInstalling.get(os).setUrlForInstalling(URI.create(currUrl).toURL());
            dataForInstalling.get(os).setPathToPandoc(Path.of(pandocDir.toString() + currPath));
            log.info("Init {} url : {}, path to pandoc: {}", os, currUrl, dataForInstalling.get(os).getPathToPandoc());
        }
    }

    /**
     * Install last released pandoc from github
     * @return path to executable pandoc
     * @throws IOException in case incorrect github url or path of installation directory
     */
    Path installPandoc() throws IOException {
        log.info("Start install");
        Path res;
        if (OS.isFamilyMac()) {
            if (OS.isArch("aarch64")) {
                res = installPandoc(OSType.MAC_OS_ARM);
            } else {
                res = installPandoc(OSType.MAC_OS_AMD);
            }
        } else if (OS.isFamilyUnix()) {
            if (OS.isArch("aarch64")) {
                res = installPandoc(OSType.LINUX_ARM);
            } else {
                res = installPandoc(OSType.LINUX_AMD);
            }
        } else if (OS.isFamilyWindows()) {
            res = installPandoc(OSType.WINDOWS);
        } else {
            throw new RuntimeException("Got unexpected os, could not install pandoc");
        }
        return res;
    }

    private Path installPandoc(OSType os) throws IOException {
        log.info(
                "Start installing pandoc os: {}, url: {}, file: {}",
                os,
                dataForInstalling.get(os).getUrlForInstalling(),
                dataForInstalling.get(os).getFileToInstall()
        );

        clearInstallingDirectory();

        if (!handleSaving(os)) {
            throw new RuntimeException("Could not save file from github");
        }
        if (!unarchive(os)) {
            throw new RuntimeException("Could not unzip file");
        }

        if (!dataForInstalling.get(os).getPathToPandoc().toFile().setExecutable(true)) {
            throw new RuntimeException("Could not make pandoc executable");
        }

        return dataForInstalling.get(os).getPathToPandoc();
    }

    /**
     * Downloads from a (http/https) URL and saves to a file.
     * @param file File to write. Parent directory will be created if necessary
     * @param url  http/https url to connect
     * @param secsConnectTimeout Seconds to wait for connection establishment
     * @param secsReadTimeout Read timeout in seconds - trasmission will abort if it freezes more than this
     * @return true if successfully save file and false if:
     *     connection interrupted, timeout (but something was read)
     *     server error (500...)
     *     could not connect: connection timeout java.net.SocketTimeoutException
     *     could not connect: java.net.ConnectException
     *     could not resolve host (bad host, or no internet - no dns)
     * @throws IOException Only if URL is malformed or if could not create the file
     * @throws FileNotFoundException if did not find file for save
     */
    private boolean saveUrl(final Path file, final URL url,
                              int secsConnectTimeout, int secsReadTimeout) throws IOException {
        Files.createDirectories(file.getParent()); // make sure parent dir exists , this can throw exception
        var conn = url.openConnection(); // can throw exception if bad url
        if (secsConnectTimeout > 0) {
            conn.setConnectTimeout(secsConnectTimeout * 1000);
        }
        if (secsReadTimeout > 0) {
            conn.setReadTimeout(secsReadTimeout * 1000);
        }
        var ret = true;
        boolean somethingRead = false;
        try (var is = conn.getInputStream()) {
            try (var in = new BufferedInputStream(is);
                 var fout = Files.newOutputStream(file)) {
                final byte data[] = new byte[8192];
                int count;
                while ((count = in.read(data)) > 0) {
                    somethingRead = true;
                    fout.write(data, 0, count);
                }
            }
        } catch (java.io.IOException e) {
            int httpcode = 999;
            try {
                httpcode = ((HttpURLConnection) conn).getResponseCode();
            } catch (Exception ee) {}

            if (e instanceof FileNotFoundException) {
                throw new FileNotFoundException("Did not found file for install");
            }

            if (somethingRead && e instanceof java.net.SocketTimeoutException) {
                log.error("Read something, but connection interrupted: {}", e.getMessage(), e);
                ret = false;
            } else if (httpcode >= 400 && httpcode < 600 ) {
                log.error("Got server error, httpcode: {}", httpcode);
                ret = false;
            } else if (e instanceof java.net.SocketTimeoutException) {
                log.error("Connection timeout: {}", e.getMessage(), e);
                ret = false;
            } else if (e instanceof java.net.ConnectException) {
                log.error("Could not connect: {}", e.getMessage(), e);
                ret = false;
            } else if (e instanceof java.net.UnknownHostException ) {
                log.error("Could not resolve host: {}", e.getMessage(), e);
                ret = false;
            } else {
                throw e;
            }
        }
        return ret;
    }

    private boolean handleSaving(OSType os) throws IOException {
        var attempt = 0;
        var saveFile = false;

        while (attempt < ATTEMPTS && !saveFile) {
            ++attempt;
            saveFile = saveUrl(
                    dataForInstalling.get(os).getFileToInstall(),
                    dataForInstalling.get(os).getUrlForInstalling(),
                    TIMEOUT_SECONDS,
                    TIMEOUT_SECONDS);
        }

        return saveFile;
    }

    private boolean unarchive(OSType os) {
        try {
            switch (os) {
                case LINUX_AMD:
                case LINUX_ARM :
                    unTarGz(dataForInstalling.get(os).getFileToInstall(), pandocDir);
                    break;
                default :
                    unZip(dataForInstalling.get(os).getFileToInstall(), pandocDir);
            }
        } catch (IOException e) {
            log.error("Could not perform unarchiving: {}", e.getMessage(), e);
            return false;
        }
        return true;
    }
    private void unTarGz(Path pathInput, Path targetDir) throws IOException {
        try (var tarIn =
                new TarArchiveInputStream(
                        new GzipCompressorInputStream(
                                new BufferedInputStream(Files.newInputStream(pathInput))))) {
            ArchiveEntry archiveEntry;
            while ((archiveEntry = tarIn.getNextEntry()) != null) {
                var pathEntryOutput = targetDir.resolve(archiveEntry.getName());
                if (archiveEntry.isDirectory()) {
                    Files.createDirectory(pathEntryOutput);
                } else {
                    Files.copy(tarIn, pathEntryOutput);
                }
            }
        }
    }

    private void unZip(Path pathInput, Path targetDir) throws IOException {
        ZipFile zipFile = new ZipFile(pathInput.toFile());
        try {
            Enumeration<ZipArchiveEntry> entries = zipFile.getEntries();
            while (entries.hasMoreElements()) {
                ZipArchiveEntry zipEntry = entries.nextElement();
                var pathEntryOutput = targetDir.resolve(zipEntry.getName());
                if (zipEntry.isDirectory()) {
                    Files.createDirectories(pathEntryOutput);
                } else {
                    Files.createDirectories(pathEntryOutput.getParent());
                    Files.copy(zipFile.getInputStream(zipEntry), pathEntryOutput);
                }
            }
        } finally {
            zipFile.close();
        }
    }


    /**
     * Clear installing directory
     */
    public static void clearInstallingDirectory() {
        try {
            FileUtils.cleanDirectory(pandocDir.toFile());
        } catch (IOException e) {
            log.error("Could not clean installing directory");
        }
    }

    /**
     * Set directory to install pandoc
     * @param newDir
     */
    public void setInstallingDirectory(Path newDir) {
        pandocDir = newDir.toAbsolutePath();
    }

    private ResponseDto getGithubUrls() throws IOException, InterruptedException {
        var uri = URI.create(properties.getProperty("github.url"));
        var client = HttpClient.newHttpClient();
        var request = HttpRequest
                .newBuilder()
                .uri(uri)
                .version(HttpClient.Version.HTTP_2)
                .timeout(Duration.ofMinutes(1))
                .header("Accept",  "application/vnd.github+json")
                .GET()
                .build();
        var response = client.send(request, HttpResponse.BodyHandlers.ofString());
        log.info("Got response from github, status: {}", response.statusCode());

        var objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.USE_JAVA_ARRAY_FOR_JSON_ARRAY, true);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        return objectMapper.readValue(response.body(), ResponseDto.class);

    }
}
