package space.kscience.snark.storage

import java.nio.file.Path

public interface Directory : AutoCloseable {
    @Deprecated(
        message = "Use Path, not String",
        level = DeprecationLevel.WARNING,
    )
    public suspend fun get(filename: String): FileReader
    // get file from subtree
    public suspend fun get(filename: Path): FileReader

    public suspend fun create(filename: String, ignoreIfExists: Boolean = false)
    @Deprecated(
        message = "Use Path, not String",
        level = DeprecationLevel.WARNING,
    )
    public suspend fun put(filename: String): FileWriter
    // put file to subtree
    public suspend fun put(filename: Path): FileWriter

    public suspend fun getSubdir(path: Path): Directory
    public suspend fun createSubdir(dirname: String, ignoreIfExists: Boolean = false): Directory

    public val path: Path
}

public interface FileReader : AutoCloseable {
    public suspend fun readAll(): ByteArray
}

public interface FileWriter : AutoCloseable {
    public suspend fun write(bytes: ByteArray)
}
