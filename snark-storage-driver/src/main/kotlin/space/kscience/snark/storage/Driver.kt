package space.kscience.snark.storage

import java.nio.file.Path

public interface Directory : AutoCloseable {
    public suspend fun get(filename: String): FileReader?

    public suspend fun create(filename: String, ignoreIfExists: Boolean = false)
    public suspend fun put(filename: String): FileWriter?

    public suspend fun getSubdir(path: Path): Directory?
    public suspend fun createSubdir(dirname: String, ignoreIfExists: Boolean = false): Directory?
}

public interface FileReader : AutoCloseable {
    public suspend fun readAll(): ByteArray
}

public interface FileWriter : AutoCloseable {
    public suspend fun write(bytes: ByteArray)
}
