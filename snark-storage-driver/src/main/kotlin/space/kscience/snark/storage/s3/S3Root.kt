package space.kscience.snark.storage.s3

import aws.sdk.kotlin.services.s3.*
import space.kscience.snark.storage.Directory
import space.kscience.snark.storage.FileReader
import space.kscience.snark.storage.FileWriter
import java.io.File
import java.lang.Exception
import java.nio.file.Path
import kotlin.io.path.*

public fun s3Storage(client: S3Client): Directory =
    S3Root(client)

public fun s3Bucket(client: S3Client, bucket: String): Directory =
    S3Directory(client, bucket, Path(""))

internal fun splitPathIntoBucketAndPath(path: Path): Pair<String, Path> {
    val bucket = path.getName(0)
    val recent = path.relativize(bucket)
    return Pair(bucket.toString(), recent)
}

internal class S3Root(private val client: S3Client) : Directory {
    override suspend fun get(filename: String): FileReader {
        throw NoSuchFileException(File(filename))
    }

    override suspend fun create(filename: String, ignoreIfExists: Boolean) {
        throw NoSuchFileException(File(filename))
    }

    override suspend fun put(filename: String): FileWriter {
        throw NoSuchFileException(File(filename))
    }

    override suspend fun getSubdir(path: Path): Directory = try {
        val (bucketName, recentPath) = splitPathIntoBucketAndPath(path)
        client.headBucket {
            bucket = bucketName
        }
        S3Directory(client, bucketName, recentPath)
    } catch (ex: Exception) {
        throw AccessDeniedException(path.toFile(), reason = ex.message)
    }

    override suspend fun createSubdir(dirname: String, ignoreIfExists: Boolean): Directory = try {
        val (bucketName, recentPath) = splitPathIntoBucketAndPath(Path(dirname))
        client.createBucket {
            bucket = bucketName
        }
        S3Directory(client, bucketName, recentPath)
    } catch (ex: Exception) {
        throw AccessDeniedException(File(dirname), reason = ex.message)
    }

    override fun close() {
    }

}