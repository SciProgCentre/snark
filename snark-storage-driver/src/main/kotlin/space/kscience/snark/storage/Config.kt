package space.kscience.snark.storage

import aws.sdk.kotlin.services.s3.S3Client
import space.kscience.snark.storage.local.localStorage
import space.kscience.snark.storage.s3.s3Bucket
import space.kscience.snark.storage.s3.s3Storage
import java.nio.file.Path

private const val DEFAULT_REGION = "arctic-vault"

public sealed interface Config {
    public fun build(): Directory
}

public data class LocalConfig(val path: Path): Config {
    override fun build(): Directory {
        return localStorage(path)
    }
}

/*
 * `~/.aws/credentials.json file is required
 */
internal fun buildS3Client(regionSpec: String): S3Client {
    return S3Client {
        region = regionSpec
    }
}

public data class S3BucketConfig(val bucketName: String, val region: String = DEFAULT_REGION): Config {
    override fun build(): Directory {
        return s3Bucket(buildS3Client(region), bucketName)
    }
}

public data class S3ServiceConfig(val region: String = DEFAULT_REGION): Config {
    override fun build(): Directory {
        return s3Storage(buildS3Client(region))
    }
}
