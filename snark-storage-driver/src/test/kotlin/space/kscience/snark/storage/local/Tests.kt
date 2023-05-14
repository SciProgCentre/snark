package space.kscience.snark.storage.local

import kotlinx.coroutines.runBlocking

import space.kscience.snark.storage.*
import java.io.File
import java.nio.file.Path
import kotlin.io.path.*
import kotlin.test.*

internal class LocalDriverTests {

    private var tempDir: Path? = null
    private var testSample: Directory? = null
    private val bytes = byteArrayOf(0, 1, 2, 3, 4, 5, 6, 7)

    @BeforeTest
    fun setUp() {
        tempDir = createTempDirectory()
        testSample = localStorage(tempDir!!)
    }

    @Test
    fun testCreate() = runBlocking {
        //folder is empty
        assertEquals(0, tempDir!!.listDirectoryEntries().size)

        //create first file
        testSample!!.create("tmp1")
        val entries = tempDir!!.listDirectoryEntries()
        assertEquals(1, entries.size)
        assertEquals(tempDir!! / Path("tmp1"), entries.first())
        //assertTrue(!entries.first().isDirectory())

        //create second file
        testSample!!.create("tmp2")
        assertEquals(2, tempDir!!.listDirectoryEntries().size)

        //check exception after duplication
        try {
            testSample!!.create("tmp1")
            fail("shouldn't ignore duplicates here")
        } catch (ex: java.nio.file.FileAlreadyExistsException) {}

        //check ignorance
        try {
            testSample!!.create("tmp1", true)
        } catch (ex: java.nio.file.FileAlreadyExistsException) {
            fail("should ignore duplicates here")
        }
    }

    @Test
    fun testPutGet() = runBlocking {
        testSample!!.create("tmp")
        testSample!!.put("tmp").write(bytes)
        assertContentEquals(bytes, testSample!!.get("tmp").readAll())
    }

    @Test
    fun testCreateSubdir() = runBlocking {
        //folder is empty
        assertEquals(0, tempDir!!.listDirectoryEntries().size)

        //create first file
        testSample!!.createSubdir("tmp1")
        val entries = tempDir!!.listDirectoryEntries()
        assertEquals(1, entries.size)
        assertEquals(tempDir!! / Path("tmp1"), entries.first())
        assertTrue (entries.first().isDirectory())

        //create second file
        testSample!!.createSubdir("tmp2")
        assertEquals(2, tempDir!!.listDirectoryEntries().size)

        //check exception after duplication
        try {
            testSample!!.createSubdir("tmp1")
            fail("shouldn't ignore duplicates here")
        } catch (ex: java.nio.file.FileAlreadyExistsException) {}

        //check ignorance
        try {
            testSample!!.createSubdir("tmp1", true)
        } catch (ex: java.nio.file.FileAlreadyExistsException) {
            fail("should ignore duplicates here")
        }
        assertTrue {true}
    }

    @Test
    fun testGetSubdir() = runBlocking {
        testSample!!.createSubdir("tmp")
        val pathStr = (Path("tmp") / "data.txt").toString()
        testSample!!.create(pathStr)
        testSample!!.put(pathStr).write(bytes)
        val subdir = testSample!!.getSubdir(Path("tmp"))
        assertContentEquals(bytes, subdir.get("data.txt").readAll())
    }

    @AfterTest
    fun tearDown() {
        tempDir!!.toFile().deleteRecursively()
    }
}