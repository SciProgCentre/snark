package space.kscience.snark.storage.s3

import aws.sdk.kotlin.services.s3.*
import space.kscience.snark.storage.Directory
import space.kscience.snark.storage.FileReader
import space.kscience.snark.storage.FileWriter
import java.lang.Exception
import java.nio.file.Path
import kotlin.io.path.*

public fun s3Storage(client: S3Client): Directory =
    S3Root(client)

public fun s3Bucket(client: S3Client, bucket: String): Directory =
    S3Directory(client, bucket, Path(""))

internal fun splitPathIntoBucketAndPath(path: Path): Pair<String, Path> {
    val bucket = path.getName(0)
    val filePath = path.relativize(bucket)
    return Pair(bucket.toString(), filePath)
}

internal class S3Root(private val client: S3Client) : Directory {
    override suspend fun get(filename: String): FileReader {
        throw NoSuchFileException(Path(filename).toFile())
    }

    override suspend fun get(filename: Path): FileReader {
        throw NoSuchFileException(filename.toFile())
    }

    override suspend fun create(filename: String, ignoreIfExists: Boolean) {
        throw NoSuchFileException(Path(filename).toFile())
    }

    override suspend fun put(filename: String): FileWriter {
        throw NoSuchFileException(Path(filename).toFile())
    }

    override suspend fun put(filename: Path): FileWriter {
        throw NoSuchFileException(filename.toFile())
    }

    override suspend fun getSubdir(path: Path): Directory = try {
        val (bucketName, filePath) = splitPathIntoBucketAndPath(path)
        client.headBucket {
            bucket = bucketName
        }
        S3Directory(client, bucketName, filePath)
    } catch (ex: Exception) {
        throw AccessDeniedException(path.toFile(), reason = ex.message)
    }

    override suspend fun createSubdir(dirname: String, ignoreIfExists: Boolean): Directory = try {
        val (bucketName, filePath) = splitPathIntoBucketAndPath(Path(dirname))
        client.createBucket {
            bucket = bucketName
        }
        S3Directory(client, bucketName, filePath)
    } catch (ex: Exception) {
        throw AccessDeniedException(Path(dirname).toFile(), reason = ex.message)
    }

    override val path: Path
        get() = Path("")

    override fun close() {
    }

}