package space.kscience.snark.storage

import java.nio.file.Path

public interface Directory : AutoCloseable {
    @Deprecated("Use Path, not String")
    public suspend fun get(filename: String): FileReader

    // get file from subtree
    public suspend fun get(filename: Path): FileReader

    @Deprecated("Use put")
    public suspend fun create(filename: String, ignoreIfExists: Boolean = false)

    @Deprecated("Use Path, not String")
    public suspend fun put(filename: String): FileWriter

    // put file to subtree
    public suspend fun put(filename: Path): FileWriter

    public suspend fun getSubdir(path: Path): Directory

    public suspend operator fun div(path: Path): Directory = getSubdir(path)

    @Deprecated("Directories are created on put")
    public suspend fun createSubdir(dirname: String, ignoreIfExists: Boolean = false): Directory

    @Deprecated("Not a good idea")
    public val path: Path
}

public interface FileReader : AutoCloseable {
    public suspend fun readAll(): ByteArray
}

public interface FileWriter : AutoCloseable {
    public suspend fun write(bytes: ByteArray)
}
