package space.kscience.snark.storage.s3

import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.GetObjectRequest
import aws.sdk.kotlin.services.s3.model.ListObjectsRequest
import aws.sdk.kotlin.services.s3.putObject
import aws.smithy.kotlin.runtime.client.LogMode
import aws.smithy.kotlin.runtime.client.endpoints.Endpoint
import aws.smithy.kotlin.runtime.client.endpoints.EndpointProvider
import aws.smithy.kotlin.runtime.content.ByteStream
import aws.smithy.kotlin.runtime.content.toByteArray
import aws.smithy.kotlin.runtime.net.Url
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import space.kscience.snark.storage.*
import kotlin.io.path.Path
import kotlin.test.Ignore
import kotlin.test.assertEquals

fun buildS3Client(regionSpec: String, endpointUrlSpec: Url): S3Client {
    return S3Client {
        endpointProvider = EndpointProvider { _ -> Endpoint(endpointUrlSpec) }
        region = regionSpec
        logMode = LogMode.LogRequest
    }
}

class Tests {
    @Test
    fun listBuckets() = runBlocking {
        val client = buildS3Client("arctic-vault", DEFAULT_ENDPOINT_URL)
        val buckets = client.listBuckets().buckets ?: emptyList()
        println("buckets: $buckets")
        assert(buckets.isNotEmpty())
    }

    @Ignore
    @Test
    fun listObjects() = runBlocking {
        val client = buildS3Client("arctic-vault", DEFAULT_ENDPOINT_URL)
        val objects = client.listObjects(ListObjectsRequest {
            bucket = "snark-test"
        }).contents ?: emptyList()
        println("objects: $objects")
        assert(objects.isNotEmpty())
    }

    @Ignore
    @Test
    fun putObject() = runBlocking {
        val client = buildS3Client("arctic-vault", DEFAULT_ENDPOINT_URL)
        client.putObject {
            bucket = "snark-test"
            body = ByteStream.fromString("Hello")
            key = "file.txt"
        }
        assert(true)
    }

    @Ignore
    @Test
    fun putFile() = runBlocking {
        val client = s3Bucket(buildS3Client("arctic-vault", DEFAULT_ENDPOINT_URL), "snark-test")
        val filepath = Path("test/testfile.txt")
        client.put(filepath).write("Hello".toByteArray())
        assertEquals("Hello", client.get(filepath).readAll().decodeToString())
    }

    @Ignore
    @Test
    fun getObject() = runBlocking {
        val client = buildS3Client("arctic-vault", DEFAULT_ENDPOINT_URL)
        client.getObject(GetObjectRequest{
            bucket = "snark-test"
            key = "file.txt"
        }) {
            assertEquals("Hello from s3\n", it.body?.toByteArray()?.decodeToString())
        }
    }

    @Ignore
    @Test
    fun getFile() = runBlocking {
        val client = s3Bucket(buildS3Client("arctic-vault", DEFAULT_ENDPOINT_URL), "snark-test")
        val filepath = Path("file.txt")
        client.get(filepath).readAll()
        assert(true)
    }
}