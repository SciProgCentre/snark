import io.ktor.server.application.Application
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.routing.routing
import space.kscience.dataforge.context.Context
import space.kscience.dataforge.context.request
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.asName
import space.kscience.dataforge.workspace.directory
import space.kscience.snark.html.SnarkHtml
import space.kscience.snark.html.document.document
import space.kscience.snark.html.document.fragment
import space.kscience.snark.html.readSiteData
import space.kscience.snark.ktor.site
import kotlin.io.path.Path
import kotlin.io.path.exists

@Suppress("unused")
fun Application.documents(context: Context) {
    val snark = context.request(SnarkHtml)
    val dataDirectory = Path("data")

    if(!dataDirectory.exists()){
        error("Data directory at $dataDirectory is not resolved")
    }

    val siteData = snark.readSiteData(context) {
        directory(snark.io, Name.EMPTY, dataDirectory)
    }

    routing {
        site(context, siteData){
            document("loremIpsum".asName()){
                fragment("chapter1")
                fragment("chapter2")
                fragment("chapter3")
            }
        }
    }
}


fun main() {
    val context = Context {
        plugin(SnarkHtml)
    }

    embeddedServer(CIO) {
        documents(context)
    }.start(true)
}