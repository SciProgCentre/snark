package space.kscience.snark.main

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class Test {
    @Test
    fun justWorks() = runBlocking {
        delay(5)
        main()
    }
}