package space.kscience.snark.storage.local

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeAll
import space.kscience.snark.storage.*
import java.nio.file.Path
import kotlin.io.path.createTempDirectory
import kotlin.io.path.deleteExisting
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.io.path.*
import kotlin.test.assertEquals

class Example {
    var tempDir: Path? = null
    var somedir: Directory? = null

    @BeforeTest
    fun setUp() {
        tempDir = createTempDirectory()
        somedir = localStorage(tempDir!!)
    }

    @AfterTest
    fun tearDown() {
        tempDir!!.toFile().deleteRecursively()
        somedir = null
    }

    @Test
    fun exampleTest() = runBlocking {
        somedir!!.put(Path("somefile")).write("hello".toByteArray())
        assertEquals("hello", somedir!!.get(Path("somefile")).readAll().decodeToString())
    }

    @Test
    fun subdirExample() = runBlocking {
        val dir1 = somedir!! / "tmp1"
        dir1.put("somefile").write("hello".toByteArray())

        val dir2 = somedir!! / "tmp1"
        val data = dir2.get("somefile").readAll()

        assertEquals("hello", data.decodeToString())
    }
}