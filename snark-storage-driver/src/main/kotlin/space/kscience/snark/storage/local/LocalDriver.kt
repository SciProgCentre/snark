package space.kscience.snark.storage.local

import space.kscience.snark.storage.Directory
import space.kscience.snark.storage.FileReader
import space.kscience.snark.storage.FileWriter
import java.io.File
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermission
import kotlin.io.path.*

public fun localStorage(rootPath: Path): Directory {
    return LocalDirectory(rootPath, Path(""))
}

internal class LocalFile(private val path: Path) : FileReader, FileWriter {
    override fun close() {}
    override suspend fun readAll(): ByteArray = path.readBytes()

    override suspend fun write(bytes: ByteArray) = path.writeBytes(bytes)
}

internal class LocalDirectory(private val root: Path, private val currentDir: Path) : Directory {
    private fun child(child: String): Path = root / currentDir / child
    private fun child(child: Path): Path = root / currentDir / child

    override fun close() {}

    override suspend fun get(filename: String): LocalFile = LocalFile(child(filename))

    override suspend fun get(filename: Path): LocalFile = LocalFile(child(filename))

    override suspend fun create(filename: String, ignoreIfExists: Boolean) {
        val dir = child(filename)
        dir.parent.createDirectories()
        try {
            child(filename).createFile()
        } catch (ex: java.nio.file.FileAlreadyExistsException) {
            if (!ignoreIfExists) {
                throw ex
            }
        }
    }

    override suspend fun put(filename: String): LocalFile = get(filename)

    override suspend fun put(filename: Path): LocalFile = get(filename)

    override suspend fun getSubdir(path: Path): LocalDirectory = LocalDirectory(root, currentDir / path)
    override suspend fun createSubdir(dirname: String, ignoreIfExists: Boolean): LocalDirectory {
        val dir = child(dirname)
        dir.parent.createDirectories()
        try {
            dir.createDirectory()
        } catch (ex: java.nio.file.FileAlreadyExistsException) {
            if (!ignoreIfExists) {
                throw ex
            }
        }
        return LocalDirectory(root, currentDir / dirname)
    }

    override val path: Path
        get() = currentDir
}
