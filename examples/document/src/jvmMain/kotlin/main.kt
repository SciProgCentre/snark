import io.ktor.server.application.Application
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.names.asName
import space.kscience.snark.html.document.document
import space.kscience.snark.html.document.fragment

@Suppress("unused")
fun Application.documents() = snarkApplication {
    document("loremIpsum".asName(), Meta { "metaValue" put "Hello world!" }) {
        fragment("chapter1")
        fragment("chapter2")
        fragment("chapter3")
    }
}


fun main() {

    embeddedServer(CIO) {
        documents()
    }.start(true)
}