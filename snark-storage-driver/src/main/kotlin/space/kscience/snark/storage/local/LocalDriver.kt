package space.kscience.snark.storage.local

import space.kscience.snark.storage.Directory
import space.kscience.snark.storage.FileReader
import space.kscience.snark.storage.FileWriter
import java.lang.Exception
import java.nio.file.Path
import kotlin.io.path.*

public fun localStorage(rootPath: Path): Directory {
    return LocalDirectory(rootPath, Path(""))
}

internal class LocalFile(private val path: Path) : FileReader, FileWriter {
    override fun close() {}
    override suspend fun readAll(): ByteArray = path.readBytes()

    override suspend fun write(bytes: ByteArray) {
        path.parent.createDirectories()
        try {
            path.createFile()
        } catch (ex: Exception) {
            // Do nothing
        }
        path.writeBytes(bytes)
    }
}

internal class LocalDirectory(private val root: Path, private val currentDir: Path) : Directory {
    @Deprecated("Use Path, not String")
    private fun realpath(child: String): Path = root / currentDir / child
    private fun realpath(child: Path): Path = root / currentDir / child

    override fun close() {}

    override suspend fun get(filename: Path): LocalFile = LocalFile(realpath(filename))

    @Deprecated("Use put")
    override suspend fun create(filename: String, ignoreIfExists: Boolean) {
        val dir = realpath(filename)
        dir.parent.createDirectories()
        try {
            realpath(filename).createFile()
        } catch (ex: java.nio.file.FileAlreadyExistsException) {
            if (!ignoreIfExists) {
                throw ex
            }
        }
    }

    override suspend fun put(filename: Path): LocalFile = get(filename)

    override suspend fun getSubdir(path: Path): LocalDirectory = LocalDirectory(root, currentDir / path)

    @Deprecated("Directories are created on put")
    override suspend fun createSubdir(dirname: String, ignoreIfExists: Boolean): LocalDirectory {
        val dir = realpath(dirname)
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

    @Deprecated("Not a good idea")
    override val path: Path
        get() = currentDir
}
