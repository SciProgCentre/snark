package space.kscience.snark.storage

import org.junit.jupiter.api.Test
import kotlin.io.path.createTempDirectory

class JustCreates {

    @Test
    fun s3Created() {
        val dir = Directory.fromConfig(S3ServiceConfig())
        dir.close()
    }

    @Test
    fun s3BucketCreated() {
        val dir = Directory.fromConfig(S3BucketConfig("snark-test"))
        dir.close()
    }

    @Test
    fun localCreated() {
        val dir = Directory.fromConfig(LocalConfig(createTempDirectory("snark-test")))
        dir.close()
    }
}