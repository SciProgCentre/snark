package space.kscience.snark.storage.unzip

import kotlinx.coroutines.runBlocking

import space.kscience.snark.storage.Directory
import space.kscience.snark.storage.local.LocalDirectory
import space.kscience.snark.storage.local.localStorage
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
        dir.put(filename).write(content)
    }

    private fun zipAll(directory: String, zipFile: String) {
        val sourceFile = File(directory)

        ZipOutputStream(BufferedOutputStream( FileOutputStream(zipFile))).use {
            zipFiles(it, sourceFile, File.separator)
            it.closeEntry()
            it.close()
        }
    }

    private fun zipFiles(zipOut: ZipOutputStream, sourceFile: File, parentDirname: String) {

        val data = ByteArray(2048)

        for (f in sourceFile.listFiles()) {
            if (f.isDirectory) {
                zipFiles(zipOut, f, parentDirname + f.name + File.separator)
            } else {
                FileInputStream(f).use { fi ->
                    BufferedInputStream(fi).use { origin ->
                        var path = parentDirname + f.name
                        val entry = ZipEntry(path.drop(1))
                        entry.time = f.lastModified()
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


    @Test
    fun testUnzip() = runBlocking {
        val dir: Directory = localStorage(tempDir!!)
        val source = dir.createSubdir("source")
        val target = dir.createSubdir("target")
        val bytes1 = byteArrayOf(0, 1, 2, 3)
        val bytes2 = byteArrayOf(1, 0, 3, 2)
        val bytes3 = byteArrayOf(3, 2, 1, 0)
        makeFile(source, "tmp1", bytes1)
        makeFile(source, "tmp2", bytes2);
        makeFile(source, (Path("tdir") / "tmp3").toString(), bytes3)

        dir.create("archive.zip")
        val archive_path = (tempDir!! / Path("archive.zip")).toString()

        zipAll((tempDir!! / Path("source")).toString(), archive_path)

        unzip(archive_path, target)

        val targetPath = tempDir!! / Path("target")
        println(targetPath)
        val entries = targetPath.listDirectoryEntries()

        assertEquals(3, entries.size)
        val exp_entries = listOf(
            targetPath / Path("tmp1"),
            targetPath / Path("tmp2"),
            targetPath / Path("tdir"))
        assertContentEquals(entries.sorted(), exp_entries.sorted())

        val tdirEntries = (targetPath / Path("tdir")).listDirectoryEntries()
        assertEquals(1, tdirEntries.size)
        assertEquals(tdirEntries.first(), targetPath / Path("tdir") / Path("tmp3"))
    }

    @AfterTest
    fun tearDown() {
        tempDir!!.toFile().deleteRecursively()
    }
}