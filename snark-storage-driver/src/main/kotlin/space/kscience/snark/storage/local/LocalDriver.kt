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

private class LocalDirectory(private val root: Path, private val currentDir: Path) : Directory {
    private fun child(child: String): Path = root / currentDir / child
    private fun child(child: Path): Path = root / currentDir / child

    override fun close() {}

    override suspend fun get(filename: String): FileReader = LocalFile(child(filename))

    override suspend fun get(filename: Path): FileReader = LocalFile(child(filename))

    override suspend fun create(filename: String, ignoreIfExists: Boolean) {
        child(filename).parent.createDirectories()

        try {
            child(filename).createFile()
        } catch (ex: java.nio.file.FileAlreadyExistsException) {
            if (!ignoreIfExists) {
                throw ex
            }
        }
    }

    override suspend fun put(filename: String): FileWriter {
        val tmp = child(filename)
        //tmp.toFile().setWritable(true)
        return LocalFile(tmp)
    }


    override suspend fun put(filename: Path): FileWriter = LocalFile(child(filename))

    override suspend fun getSubdir(path: Path): LocalDirectory = LocalDirectory(root, child(path))
    override suspend fun createSubdir(dirname: String, ignoreIfExists: Boolean): LocalDirectory {
        val dir = child(dirname)
        try {
            dir.createDirectory()
        } catch (ex: java.nio.file.FileAlreadyExistsException) {
            if (!ignoreIfExists) {
                throw ex
            }
        }
        return LocalDirectory(root, dir)
    }

    override val path: Path
        get() = currentDir
}
