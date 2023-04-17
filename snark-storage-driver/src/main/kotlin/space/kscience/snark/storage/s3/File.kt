package space.kscience.snark.storage.s3

import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.GetObjectRequest
import aws.sdk.kotlin.services.s3.putObject
import aws.smithy.kotlin.runtime.content.ByteStream
import aws.smithy.kotlin.runtime.content.toByteArray
import space.kscience.snark.storage.FileReader
import space.kscience.snark.storage.FileWriter

public class S3FileReader(private val client: S3Client, private val bucketName: String, private val fullQualifiedPath: String) : FileReader {
    override suspend fun readAll(): ByteArray {
        val result = client.getObject(GetObjectRequest{
            bucket = bucketName
            key = fullQualifiedPath
        }) {
            it.body?.toByteArray() ?: ByteArray(0)
        }
        return result
    }

    override fun close() {
    }
}

public class S3FileWriter(private val client: S3Client, private val bucketName: String, private val fullQualifiedPath: String) : FileWriter {
    override suspend fun write(bytes: ByteArray) {
        client.putObject {
            bucket = bucketName
            key = fullQualifiedPath
            body = ByteStream.fromBytes(bytes)
        }
    }

    override fun close() {
    }

}