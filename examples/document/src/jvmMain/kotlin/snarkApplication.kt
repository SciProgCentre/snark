import io.ktor.server.application.Application
import io.ktor.server.application.log
import io.ktor.server.http.content.staticResources
import io.ktor.server.routing.routing
import space.kscience.dataforge.context.Context
import space.kscience.dataforge.context.ContextBuilder
import space.kscience.dataforge.context.request
import space.kscience.dataforge.data.forEach
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.workspace.FileData
import space.kscience.dataforge.workspace.directory
import space.kscience.snark.html.HtmlSite
import space.kscience.snark.html.SnarkHtml
import space.kscience.snark.html.readSiteData
import space.kscience.snark.ktor.site
import kotlin.io.path.Path
import kotlin.io.path.exists


fun Application.snarkApplication(contextBuilder: ContextBuilder.() -> Unit = {}, site: HtmlSite) {

    val context = Context {
        plugin(SnarkHtml)
        contextBuilder()
    }

    val snark = context.request(SnarkHtml)

    val dataDirectoryString = environment.config.propertyOrNull("snark.dataDirectory")?.getString() ?: "data"

    val dataDirectory = Path(dataDirectoryString)

    if (!dataDirectory.exists()) {
        error("Data directory at $dataDirectory is not resolved")
    }

    val siteData = snark.readSiteData(context) {
        directory(snark.io, Name.EMPTY, dataDirectory)
    }

    siteData.forEach { namedData ->
        log.debug("Loading data {} from {}", namedData.name, namedData.meta[FileData.FILE_PATH_KEY])
    }

    routing {
        staticResources("/css","css")
        site(context, siteData, content = site)
    }
}