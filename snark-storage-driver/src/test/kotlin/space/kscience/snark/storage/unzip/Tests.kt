package space.kscience.snark.storage.unzip

import kotlinx.coroutines.runBlocking

import space.kscience.snark.storage.Directory
import space.kscience.snark.storage.local.LocalDirectory
import java.io.*
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.*
import kotlin.test.*

internal class UnzipTests {

    private var tempDir: Path? = null

    @BeforeTest
    fun setUp() {
        tempDir = createTempDirectory()
    }

    private suspend fun makeFile(dir: Directory, filename: String, content: ByteArray) {
        dir.create(filename)

        val writter = dir.put(filename)
        if (!(tempDir!! / Path("source") / Path(filename)).isRegularFile()) {
            println("shit")
        }
        writter.write(content)
    }

    private fun zipAll(directory: String, zipFile: String) {
        val sourceFile = File(directory)

        ZipOutputStream(BufferedOutputStream( FileOutputStream(zipFile))).use {
            it.use {
                zipFiles(it, sourceFile, "")
            }
        }
    }

    private fun zipFiles(zipOut: ZipOutputStream, sourceFile: File, parentDirPath: String) {

        val data = ByteArray(2048)

        for (f in sourceFile.listFiles()) {

            if (f.isDirectory) {
                val entry = ZipEntry(f.name + File.separator)
                entry.time = f.lastModified()
                entry.isDirectory
                entry.size = f.length()
                zipOut.putNextEntry(entry)
                zipFiles(zipOut, f, f.name)
            } else {
                if (!f.name.contains(".zip")) { //If folder contains a file with extension ".zip", skip it
                    FileInputStream(f).use { fi ->
                        BufferedInputStream(fi).use { origin ->
                            val path = parentDirPath + File.separator + f.name
                            val entry = ZipEntry(path)
                            entry.time = f.lastModified()
                            entry.isDirectory
                            entry.size = f.length()
                            zipOut.putNextEntry(entry)
                            while (true) {
                                val readBytes = origin.read(data)
                                if (readBytes == -1) {
                                    break
                                }
                                zipOut.write(data, 0, readBytes)
                            }
                        }
                    }
                }
            }
        }
    }


    @Test
    fun testUnzip() = runBlocking {
        val dir: Directory = LocalDirectory(tempDir!!)
        val source = dir.createSubdir("source")
        val target = dir.createSubdir("target")
        val bytes1 = byteArrayOf(0, 1, 2, 3)
        val bytes2 = byteArrayOf(1, 0, 3, 2)
        val bytes3 = byteArrayOf(3, 2, 1, 0)
        makeFile(source, "tmp1", bytes1)
        makeFile(source, "tmp2", bytes2);
        makeFile(source, "tdir${File.separator}tmp3", bytes3)

        dir.create("archive.zip")
        val archive_path = (tempDir!! / Path("archive.zip")).toString()

        zipAll((tempDir!! / Path("source")).toString(), archive_path)

        unzip(archive_path, target)

        val entries = (tempDir!! / Path("target")).listDirectoryEntries()
        assertEquals(3, entries.size)
    }

    @AfterTest
    fun tearDown() {
        tempDir!!.toFile().deleteRecursively()
    }
}