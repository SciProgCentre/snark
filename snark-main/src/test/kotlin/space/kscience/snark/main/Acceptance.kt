package space.kscience.snark.main

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import space.kscience.snark.ktor.SNARKServer
import space.kscience.snark.storage.local.localStorage
import kotlin.io.path.Path

class Acceptance {
    @Test
    fun justWorks() = runBlocking {
        main()
    }
}