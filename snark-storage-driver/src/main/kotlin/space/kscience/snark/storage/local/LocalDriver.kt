package space.kscience.snark.storage.local

import space.kscience.snark.storage.Directory
import space.kscience.snark.storage.FileReader
import space.kscience.snark.storage.FileWriter
import java.io.File
import java.nio.file.Path

public class LocalFile(private val path: String) : FileReader, FileWriter {
    override fun close() {}
    override suspend fun readAll(): ByteArray = File(this.path).readBytes()

    override suspend fun write(bytes: ByteArray) = File(this.path).writeBytes(bytes)
}

public class LocalDirectory(private val root: String, private val path: String) : Directory {
    private val current = "$root/$path"

    private fun child(child: String): String = "$current/$child"

    override fun close() {}

    override suspend fun get(filename: String): FileReader = LocalFile(child(filename))

    override suspend fun create(filename: String, ignoreIfExists: Boolean) {
        if (!File(child(filename)).createNewFile() && !ignoreIfExists) {
            throw UnsupportedOperationException("File already exists")
        }
    }

    override suspend fun put(filename: String): FileWriter = LocalFile(child(filename))

    override suspend fun getSubdir(path: Path): Directory = LocalDirectory(root, child(path.toString()))
    override suspend fun createSubdir(dirname: String, ignoreIfExists: Boolean): Directory {
        val dir = child(dirname)
        if (!File(dir).mkdir() && !ignoreIfExists) {
            throw UnsupportedOperationException("File already exists")
        }
        return this.getSubdir(File(dir).toPath())
    }
}
