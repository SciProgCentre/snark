package space.kscience.snark.storage.local

import space.kscience.snark.storage.Directory
import space.kscience.snark.storage.FileReader
import space.kscience.snark.storage.FileWriter
import java.nio.file.Path
import kotlin.io.path.*

public fun localStorage(rootPath: Path): Directory {
    return LocalDirectory(rootPath, Path(""))
}

private class LocalFile(private val path: Path) : FileReader, FileWriter {
    override fun close() {}
    override suspend fun readAll(): ByteArray = path.readBytes()

    override suspend fun write(bytes: ByteArray) = path.writeBytes(bytes)
}

private class LocalDirectory(private val root: Path, private val currentDir: Path) : Directory {
    private fun child(child: String): Path = root / currentDir / child
    private fun child(child: Path): Path = root / currentDir / child

    override fun close() {}

    override suspend fun get(filename: String): FileReader = LocalFile(child(filename))

    override suspend fun get(filename: Path): FileReader = LocalFile(child(filename))

    override suspend fun create(filename: String, ignoreIfExists: Boolean) {
        try {
            child(filename).createFile()
        } catch (ex: FileAlreadyExistsException) {
            if (!ignoreIfExists) {
                throw ex
            }
        }
    }

    override suspend fun put(filename: String): FileWriter = LocalFile(child(filename))

    override suspend fun put(filename: Path): FileWriter = LocalFile(child(filename))

    override suspend fun getSubdir(path: Path): Directory = LocalDirectory(root, child(path))
    override suspend fun createSubdir(dirname: String, ignoreIfExists: Boolean): Directory {
        val dir = child(dirname)
        try {
            dir.createDirectory()
        } catch (ex: FileAlreadyExistsException) {
            if (!ignoreIfExists) {
                throw ex
            }
        }
        return LocalDirectory(root, dir)
    }

    override val path: Path
        get() = currentDir
}
