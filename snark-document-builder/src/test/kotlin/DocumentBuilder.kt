package documentBuilder

import org.junit.jupiter.api.Test
import kotlinx.coroutines.runBlocking

class SomeTest {
    @Test
    fun justWorks() = runBlocking {
        buildDocument("../example")
    }
}
