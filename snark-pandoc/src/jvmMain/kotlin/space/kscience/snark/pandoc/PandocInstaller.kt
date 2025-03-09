package space.kscience.snark.pandoc

import kotlinx.serialization.json.Json
import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.exec.OS
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.BufferedInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.net.*
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermission
import java.time.Duration
import java.util.*
import java.util.zip.ZipInputStream
import kotlin.io.path.Path
import kotlin.io.path.inputStream

internal object PandocInstaller {

    private val log: Logger = LoggerFactory.getLogger(PandocInstaller::class.java)


    private const val TIMEOUT_SECONDS = 2
    private const val ATTEMPTS = 3

    private enum class OSType(val assetSuffix: String, val propertySuffix: String) {
        WINDOWS("windows-x86_64.zip", "windows"),
        MAC_OS_AMD("x86_64-macOS.zip", "mac.os.amd"),
        MAC_OS_ARM("arm64-macOS.zip", "mac.os.arm"),
        LINUX_ARM("linux-arm64", "linux.arm"),
        LINUX_AMD("linux-amd64", "linux.amd")
    }

    private val properties = Properties().apply {
        load(PandocInstaller.javaClass.getResourceAsStream("/installer.properties")!!)
    }

    /**
     * Install last released pandoc from github
     * @return path to executable pandoc
     * @throws IOException in case incorrect github url or path of installation directory
     */
    public fun installPandoc(targetPath: Path): Path {
        log.info("Start install")
        return if (OS.isFamilyMac()) {
            if (OS.isArch("aarch64")) {
                installPandoc(OSType.MAC_OS_ARM, targetPath)
            } else {
                installPandoc(OSType.MAC_OS_AMD, targetPath)
            }
        } else if (OS.isFamilyUnix()) {
            if (OS.isArch("aarch64")) {
                installPandoc(OSType.LINUX_ARM, targetPath)
            } else {
                installPandoc(OSType.LINUX_AMD, targetPath)
            }
        } else if (OS.isFamilyWindows()) {
            installPandoc(OSType.WINDOWS, targetPath)
        } else {
            error("Got unexpected os, could not install pandoc")
        }
    }


    private fun installPandoc(os: OSType, targetPath: Path): Path {

        val githubResponse = getGithubUrls()
        val asset = githubResponse.getAssetByOsSuffix(os.assetSuffix)
        val currUrl = asset.browserDownloadUrl

        val pandocUrl: URL = URI.create(currUrl).toURL()
        val fileToInstall: Path = when (os) {
            OSType.LINUX_AMD, OSType.LINUX_ARM -> Path("$targetPath/pandoc.tar.gz")
            else -> Path("$targetPath/pandoc.zip")
        }

        log.info(
            "Start installing pandoc os: {}, url: {}, file: {}",
            os,
            pandocUrl,
            fileToInstall
        )

        val archivePath = downloadWithRetry(pandocUrl) ?: error("Could not save file from github")
        val installPath = unPack(archivePath, targetPath, os) ?: error("Could not unzip file")


        val pandocExecutablePath = installPath.resolve(
            properties.getProperty("path.to.pandoc." + os.propertySuffix).replace(
                "{version}",
                githubResponse.tagName
            )
        )

        if (os == OSType.LINUX_AMD || os == OSType.LINUX_ARM) {
            Files.setPosixFilePermissions(pandocExecutablePath, setOf(PosixFilePermission.GROUP_EXECUTE))
        }

        return pandocExecutablePath
    }

    /**
     * Downloads from a (http/https) URL and saves to a file.
     * @param target File to write. Parent directory will be created if necessary
     * @param url  http/https url to connect
     * @param secsConnectTimeout Seconds to wait for connection establishment
     * @param secsReadTimeout Read timeout in seconds - trasmission will abort if it freezes more than this
     * @return true if successfully save file and false if:
     * connection interrupted, timeout (but something was read)
     * server error (500...)
     * could not connect: connection timeout java.net.SocketTimeoutException
     * could not connect: java.net.ConnectException
     * could not resolve host (bad host, or no internet - no dns)
     * @throws IOException Only if URL is malformed or if could not create the file
     * @throws FileNotFoundException if did not find file for save
     */
    @Throws(IOException::class)
    private fun downloadUrl(
        target: Path,
        url: URL,
        secsConnectTimeout: Int,
        secsReadTimeout: Int,
    ): Path? {
        Files.createDirectories(target.parent) // make sure parent dir exists , this can throw exception
        val conn = url.openConnection() // can throw exception if bad url
        if (secsConnectTimeout > 0) {
            conn.connectTimeout = secsConnectTimeout * 1000
        }
        if (secsReadTimeout > 0) {
            conn.readTimeout = secsReadTimeout * 1000
        }
        var ret = true
        var somethingRead = false
        try {
            conn.getInputStream().use { `is` ->
                BufferedInputStream(`is`).use { `in` ->
                    Files.newOutputStream(target).use { fout ->
                        val data = ByteArray(8192)
                        var count: Int
                        while ((`in`.read(data).also { count = it }) > 0) {
                            somethingRead = true
                            fout.write(data, 0, count)
                        }
                    }
                }
            }
            return target
        } catch (e: IOException) {
            var httpcode = 999
            try {
                httpcode = (conn as HttpURLConnection).responseCode
            } catch (ee: Exception) {
            }

            if (e is FileNotFoundException) {
                throw FileNotFoundException("Did not found file for install")
            }

            if (somethingRead && e is SocketTimeoutException) {
                log.error("Read something, but connection interrupted: {}", e.message, e)
            } else if (httpcode >= 400 && httpcode < 600) {
                log.error("Got server error, httpcode: {}", httpcode)
            } else if (e is SocketTimeoutException) {
                log.error("Connection timeout: {}", e.message, e)
            } else if (e is ConnectException) {
                log.error("Could not connect: {}", e.message, e)
            } else if (e is UnknownHostException) {
                log.error("Could not resolve host: {}", e.message, e)
            } else {
                throw e
            }
            return null
        }
    }

    private fun downloadWithRetry(url: URL): Path? {
        val targetPath = Files.createTempFile("pandoc", ".tmp")
        log.info("Downloading pandoc to $targetPath")

        repeat(ATTEMPTS) {
            return downloadUrl(
                targetPath,
                url,
                TIMEOUT_SECONDS,
                TIMEOUT_SECONDS
            )
        }

        return null
    }

    private fun unPack(sourcePath: Path, targetPath: Path, os: OSType): Path? {
        try {
            when (os) {
                OSType.LINUX_AMD, OSType.LINUX_ARM -> unTarGz(sourcePath, targetPath)

                else -> unZip(sourcePath, targetPath)
            }
        } catch (e: IOException) {
            log.error("Could not perform unpacking: {}", e.message, e)
            return null
        }
        return targetPath
    }

    private fun unTarGz(sourcePath: Path, targetDir: Path) {
        TarArchiveInputStream(
            GzipCompressorInputStream(
                BufferedInputStream(Files.newInputStream(sourcePath))
            )
        ).use { tarIn ->
            var archiveEntry: ArchiveEntry
            while ((tarIn.nextEntry.also { archiveEntry = it }) != null) {
                val pathEntryOutput = targetDir.resolve(archiveEntry.name)
                if (archiveEntry.isDirectory) {
                    Files.createDirectory(pathEntryOutput)
                } else {
                    Files.copy(tarIn, pathEntryOutput)
                }
            }
        }
    }

    private fun unZip(sourcePath: Path, targetDir: Path) {
        ZipInputStream(sourcePath.inputStream()).use { zis ->
            do {
                val entry = zis.nextEntry
                if (entry == null) continue
                val pathEntryOutput = targetDir.resolve(entry.name)
                if (entry.isDirectory) {
                    Files.createDirectories(pathEntryOutput)
                } else {
                    Files.createDirectories(pathEntryOutput.parent)
                    Files.copy(zis, pathEntryOutput)
                }
                zis.closeEntry()
            } while (entry != null)
        }
    }

    private fun getGithubUrls(): ResponseDto {
        val uri = URI.create(properties.getProperty("github.url"))
        val client = HttpClient.newHttpClient()
        val request = HttpRequest
            .newBuilder()
            .uri(uri)
            .version(HttpClient.Version.HTTP_2)
            .timeout(Duration.ofMinutes(1))
            .header("Accept", "application/vnd.github+json")
            .GET()
            .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        log.info("Got response from github, status: {}", response.statusCode())

        return Json { ignoreUnknownKeys = true }.decodeFromString(ResponseDto.serializer(), response.body())
    }
}
