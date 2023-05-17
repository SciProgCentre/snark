package space.kscience.snark.storage.s3

import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.GetObjectRequest
import aws.sdk.kotlin.services.s3.putObject
import aws.smithy.kotlin.runtime.content.ByteStream
import aws.smithy.kotlin.runtime.content.toByteArray
import space.kscience.snark.storage.FileReader
import space.kscience.snark.storage.FileWriter
import java.nio.file.Path

internal class S3FileReader(private val client: S3Client, private val bucketName: String, private val path: Path) :
    FileReader {
    override suspend fun readAll(): ByteArray {
        val result = client.getObject(GetObjectRequest {
            bucket = bucketName
            key = path.toString()
        }) {
            it.body?.toByteArray() ?: ByteArray(0)
        }
        return result
    }

    override fun close() {
    }
}

internal class S3FileWriter(private val client: S3Client, private val bucketName: String, private val path: Path) :
    FileWriter {
    override suspend fun write(bytes: ByteArray) {
        client.putObject {
            bucket = bucketName
            key = path.toString()
            body = ByteStream.fromBytes(bytes)
        }
    }

    override fun close() {
    }

}