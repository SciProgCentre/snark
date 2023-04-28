package space.kscience.snark.storage.local

import space.kscience.snark.storage.Directory
import space.kscience.snark.storage.FileReader
import space.kscience.snark.storage.FileWriter
import java.io.File
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermission
import kotlin.io.path.*

public fun localStorage(rootPath: Path): Directory {
    return LocalDirectory(rootPath)
}

internal class LocalFile(private val path: Path) : FileReader, FileWriter {
    override fun close() {}
    override suspend fun readAll(): ByteArray = path.readBytes()

    override suspend fun write(bytes: ByteArray) = path.writeBytes(bytes)
}

internal class LocalDirectory(private val path: Path) : Directory {
    private fun child(child: String): Path = path / child
    private fun child(child: Path): Path = path / child

    override fun close() {}

    override suspend fun get(filename: String): FileReader = LocalFile(child(filename))

    override suspend fun create(filename: String, ignoreIfExists: Boolean) {
        val parts = filename.split(File.separator)
        var cdir = this
        for (i in 0..(parts.size - 1)) {
            cdir = cdir.createSubdir(parts[i], true)
        }
        try {
            val nfile = cdir.child(parts.last()).createFile()
            /*
            nfile.toFile().setReadable(true)

            nfile.toFile().setWritable(true)
            nfile.toFile().setExecutable(true)
            */
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

    override suspend fun getSubdir(path: Path): Directory = LocalDirectory(child(path))
    override suspend fun createSubdir(dirname: String, ignoreIfExists: Boolean): LocalDirectory {
        val dir = child(dirname)
        try {
            dir.createDirectory()
        } catch (ex: java.nio.file.FileAlreadyExistsException) {
            if (!ignoreIfExists) {
                throw ex
            }
        }
        return LocalDirectory(dir)
    }
}
