package hello

import org.junit.jupiter.api.Test
import kotlinx.coroutines.runBlocking

class SomeTest {
    @Test
    fun testEssential() = runBlocking {
        doSomethingEssential()
    }
}
