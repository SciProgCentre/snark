package space.kscience.snark.storage.s3

import aws.sdk.kotlin.services.s3.*
import space.kscience.snark.storage.Directory as Dir
import space.kscience.snark.storage.FileReader
import space.kscience.snark.storage.FileWriter
import java.io.File
import java.lang.Exception
import java.nio.file.Path

public class Root(private val client: S3Client) : Dir {
    override suspend fun get(filename: String): FileReader {
        throw NoSuchFileException(File(filename))
    }

    override suspend fun create(filename: String, ignoreIfExists: Boolean) {
        throw IllegalCallerException()
    }

    override suspend fun put(filename: String): FileWriter {
        throw NoSuchFileException(File(filename))
    }

    override suspend fun getSubdir(path: Path): Directory = try {
        val bucketName = path.toString()
        client.headBucket {
            bucket = bucketName
        }
        Directory(client, bucketName, "")
    } catch (ex: Exception) {
        throw java.nio.file.AccessDeniedException(path.toString()).initCause(ex)
    }

    override suspend fun createSubdir(dirname: String, ignoreIfExists: Boolean): Directory = try {
        client.createBucket {
            bucket = dirname
        }
        Directory(client, dirname, "")
    } catch (ex: Exception) {
        throw java.nio.file.AccessDeniedException(dirname).initCause(ex)
    }

    override fun close() {
    }

}