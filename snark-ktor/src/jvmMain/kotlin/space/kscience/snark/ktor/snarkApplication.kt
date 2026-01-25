package space.kscience.snark.ktor

import io.ktor.server.application.Application
import io.ktor.server.application.log
import io.ktor.server.http.content.staticResources
import io.ktor.server.routing.Route
import io.ktor.server.routing.application
import io.ktor.server.routing.routing
import space.kscience.dataforge.context.Context
import space.kscience.dataforge.context.ContextBuilder
import space.kscience.dataforge.context.request
import space.kscience.dataforge.data.forEach
import space.kscience.dataforge.workspace.FileDataTree
import space.kscience.dataforge.workspace.readDirectory
import space.kscience.snark.html.HtmlSite
import space.kscience.snark.html.SnarkHtml
import space.kscience.snark.html.parseDataTree
import kotlin.io.path.Path
import kotlin.io.path.exists

private fun Application.defaultDataPath() = environment.config
    .propertyOrNull("snark.dataDirectory")?.getString() ?: "data"

/**
 * Create a snark site at a given route. Uses [dataPath] as a path to data directory.
 *
 * The default [dataPath] is taken from "snark.dataDirectory" property.
 * If not defined, use "data" directory in current work directory.
 */
public fun Route.site(
    contextBuilder: ContextBuilder.() -> Unit = {},
    dataPath: String = application.defaultDataPath(),
    site: HtmlSite,
) {

    val context = Context {
        plugin(SnarkHtml)
        contextBuilder()
    }

    val snark = context.request(SnarkHtml)

    val dataDirectory = Path(dataPath)

    if (!dataDirectory.exists()) {
        error("Data directory at $dataDirectory is not resolved")
    }

    val siteData = with(snark) {
        parseDataTree(io.readDirectory(dataDirectory, monitor = true, includeExtensions = true))
    }

    siteData.forEach { namedData ->
        application.log.debug("Loading data {} from {}", namedData.name, namedData.meta[FileDataTree.FILE_PATH_KEY])
    }


    debugDataTree("@data", siteData)

    staticResources("/css", "css")
    site(context, siteData, content = site)
}

/**
 * A Ktor module for snark application builder
 */
public fun Application.snarkApplication(
    contextBuilder: ContextBuilder.() -> Unit = {},
    dataPath: String = defaultDataPath(),
    site: HtmlSite,
) {
    routing {
        site(contextBuilder, dataPath, site)
    }
}