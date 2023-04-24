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

public class LocalDirectory(private val path: String) : Directory {
    override fun close() {}
    override suspend fun get(filename: String): FileReader = LocalFile("${this.path}${File.separator}$filename")

    override suspend fun create(filename: String, ignoreIfExists: Boolean) {
        if (!File("${this.path}${File.separator}$filename").createNewFile() && !ignoreIfExists) {
            throw UnsupportedOperationException("File already exists")
        }
    }

    override suspend fun put(filename: String): FileWriter = LocalFile("${this.path}${File.separator}$filename")

    override suspend fun getSubdir(dirpath: Path): Directory = LocalDirectory("${this.path}${File.separator}$dirpath")
    override suspend fun createSubdir(dirname: String, ignoreIfExists: Boolean): Directory {
        if (!File("${this.path}${File.separator}$dirname").mkdir() && !ignoreIfExists) {
            throw UnsupportedOperationException("File already exists")
        }
        return this.getSubdir(File(dirname).toPath())
    }
}
