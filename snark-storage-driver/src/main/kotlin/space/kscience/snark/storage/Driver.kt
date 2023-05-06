package space.kscience.snark.storage

import java.nio.file.Path
import kotlin.io.path.*

public interface Directory : AutoCloseable {
    // get file from subtree
    public suspend fun get(filename: Path): FileReader

    @Deprecated("Use put")
    public suspend fun create(filename: String, ignoreIfExists: Boolean = false)

    // put file to subtree
    public suspend fun put(filename: Path): FileWriter

    public suspend fun getSubdir(path: Path): Directory

    @Deprecated("Directories are created on put")
    public suspend fun createSubdir(dirname: String, ignoreIfExists: Boolean = false): Directory

    @Deprecated("Not a good idea")
    public val path: Path
}


public suspend fun Directory.get(filename: String): FileReader = get(Path(filename))

public suspend fun Directory.put(filename: String): FileWriter = put(Path(filename))

public suspend operator fun Directory.div(path: Path): Directory = getSubdir(path)

public suspend operator fun Directory.div(path: String): Directory = getSubdir(Path(path))

public interface FileReader : AutoCloseable {
    public suspend fun readAll(): ByteArray
}

public interface FileWriter : AutoCloseable {
    public suspend fun write(bytes: ByteArray)
}
