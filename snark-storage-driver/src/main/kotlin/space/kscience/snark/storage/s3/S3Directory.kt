package space.kscience.snark.storage.s3

import aws.sdk.kotlin.services.s3.S3Client
import space.kscience.snark.storage.Directory
import space.kscience.snark.storage.FileReader
import space.kscience.snark.storage.FileWriter
import java.nio.file.Path
import kotlin.io.path.*

internal class S3Directory(
    private val client: S3Client,
    private val bucketName: String,
    private val currentDir: Path,
) : Directory {
    override suspend fun get(filename: String): FileReader = run {
        S3FileReader(client, bucketName, currentDir / filename)
    }

    override suspend fun create(filename: String, ignoreIfExists: Boolean) {
        if (!ignoreIfExists) {
            throw IllegalArgumentException("could not check if file exists")
        }
    }

    override suspend fun put(filename: String): FileWriter =
        S3FileWriter(client, bucketName, currentDir / filename)

    override suspend fun getSubdir(path: Path): S3Directory =
        S3Directory(client, bucketName, currentDir / path)

    override suspend fun createSubdir(dirname: String, ignoreIfExists: Boolean): S3Directory = run {
        if (!ignoreIfExists) {
            throw IllegalArgumentException("could not check if directory exists")
        }
        S3Directory(client, bucketName, currentDir / dirname)
    }

    override fun close() {
    }
}