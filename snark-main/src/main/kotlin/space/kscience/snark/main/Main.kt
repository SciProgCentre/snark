package space.kscience.snark.main

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import space.kscience.snark.ktor.SNARKServer
import space.kscience.snark.storage.local.localStorage
import kotlin.io.path.Path

// Entrypoint
fun main(): Unit = runBlocking {
    // Parse config, create driver
    val port = 8080
    val directory = localStorage(Path("./rundata"))
    val server = SNARKServer(ServerDataHolder(directory), port)
    launch {
        server.run()
    }
}