package space.kscience.snark.ktor

import io.ktor.http.ContentType
import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import io.ktor.http.fromFileExtension
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.html.respondHtml
import io.ktor.server.http.content.staticFiles
import io.ktor.server.plugins.origin
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.Route
import io.ktor.server.routing.createRouteFromPath
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.css.CssBuilder
import kotlinx.html.CommonAttributeGroupFacade
import kotlinx.html.HTML
import kotlinx.html.head
import kotlinx.html.style
import space.kscience.dataforge.context.error
import space.kscience.dataforge.context.logger
import space.kscience.dataforge.data.DataTree
import space.kscience.dataforge.data.DataTreeItem
import space.kscience.dataforge.data.await
import space.kscience.dataforge.data.getItem
import space.kscience.dataforge.io.Binary
import space.kscience.dataforge.io.toByteArray
import space.kscience.dataforge.meta.*
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.cutLast
import space.kscience.dataforge.names.endsWith
import space.kscience.dataforge.names.plus
import space.kscience.dataforge.workspace.FileData
import space.kscience.snark.html.SiteBuilder
import space.kscience.snark.html.SnarkHtml
import space.kscience.snark.html.WebPage
import space.kscience.snark.html.toWebPath
import java.nio.file.Path
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.reflect.typeOf

public fun CommonAttributeGroupFacade.css(block: CssBuilder.() -> Unit) {
    style = CssBuilder().block().toString()
}

public class KtorSiteBuilder(
    override val snark: SnarkHtml,
    override val data: DataTree<*>,
    override val siteMeta: Meta,
    private val baseUrl: String,
    override val route: Name,
    private val ktorRoute: Route,
) : SiteBuilder {

    private fun files(item: DataTreeItem<Any>, routeName: Name) {
        //try using direct file rendering
        item.meta[FileData.FILE_PATH_KEY]?.string?.let {
            val file = try {
                Path.of(it).toFile()
            } catch (ex: Exception) {
                //failure,
                logger.error { "File $it could not be converted to java.io.File" }
                return@let
            }

            val fileName = routeName.toWebPath()
            ktorRoute.staticFiles(fileName, file)
            //success, don't do anything else
            return@files
        }
        when (item) {
            is DataTreeItem.Leaf -> {
                val datum = item.data
                if (datum.type != typeOf<Binary>()) error("Can't directly serve file of type ${item.data.type}")
                ktorRoute.get(routeName.toWebPath()) {
                    val binary = datum.await() as Binary
                    val extension = item.meta[FileData.FILE_EXTENSION_KEY]?.string?.let { ".$it" } ?: ""
                    val contentType: ContentType = extension
                        .let(ContentType::fromFileExtension)
                        .firstOrNull()
                        ?: ContentType.Any
                    call.respondBytes(contentType = contentType) {
                        //TODO optimize using streaming
                        binary.toByteArray()
                    }
                }
            }

            is DataTreeItem.Node -> {
                item.tree.items.forEach { (token, childItem) ->
                    files(childItem, routeName + token)
                }
            }
        }
    }

    override fun static(dataName: Name, routeName: Name) {
        val item: DataTreeItem<Any> = data.getItem(dataName) ?: error("Data with name $dataName is not resolved")
        files(item, routeName)
    }
//
//    override fun file(file: Path, webPath: String) {
//        if (file.isDirectory()) {
//            ktorRoute.static(webPath) {
//                //TODO check non-standard FS and convert
//                files(file.toFile())
//            }
//        } else if (webPath.isBlank()) {
//            error("Can't mount file to an empty route")
//        } else {
//            ktorRoute.file(webPath, file.toFile())
//        }
//    }

//    override fun file(dataName: Name, webPath: String) {
//        val fileData = data[dataName]
//        if(fileData is FileData){
//            ktorRoute.file(webPath)
//        }
//    }

    private fun resolveRef(baseUrl: String, ref: String) = if (baseUrl.isEmpty()) {
        ref
    } else if (ref.isEmpty()) {
        baseUrl
    } else {
        "${baseUrl.removeSuffix("/")}/$ref"
    }


    private inner class KtorWebPage(
        val pageBaseUrl: String,
        override val pageMeta: Meta,
    ) : WebPage {
        override val snark: SnarkHtml get() = this@KtorSiteBuilder.snark
        override val data: DataTree<*> get() = this@KtorSiteBuilder.data

        override fun resolveRef(ref: String): String = this@KtorSiteBuilder.resolveRef(pageBaseUrl, ref)

        override fun resolvePageRef(
            pageName: Name,
            relative: Boolean,
        ): String {
            val fullPageName = if (relative) this@KtorSiteBuilder.route + pageName else pageName
            return if (fullPageName.endsWith(SiteBuilder.INDEX_PAGE_TOKEN)) {
                resolveRef(fullPageName.cutLast().toWebPath())
            } else {
                resolveRef(fullPageName.toWebPath())
            }
        }
    }

    override fun page(route: Name, pageMeta: Meta, content: context(HTML, WebPage) () -> Unit) {
        ktorRoute.get(route.toWebPath()) {
            val request = call.request
            //substitute host for url for backwards calls
            val url = URLBuilder(baseUrl).apply {
                protocol = URLProtocol.createOrDefault(request.origin.scheme)
                host = request.origin.serverHost
                port = request.origin.serverPort
            }

            val modifiedPageMeta = pageMeta.toMutableMeta().apply {
                "name" put route.toString()
                "url" put url.buildString()
            }
            val pageBuilder = KtorWebPage(url.buildString(), Laminate(modifiedPageMeta, siteMeta))

            call.respondHtml {
                head{}
                content(this, pageBuilder)
            }
        }
    }

    override fun route(
        routeName: Name,
        dataOverride: DataTree<*>?,
        routeMeta: Meta,
    ): SiteBuilder = KtorSiteBuilder(
        snark = snark,
        data = dataOverride ?: data,
        siteMeta = Laminate(routeMeta, siteMeta),
        baseUrl = baseUrl,
        route = this.route + routeName,
        ktorRoute = ktorRoute.createRouteFromPath(routeName.toWebPath())
    )

    override fun site(
        routeName: Name,
        dataOverride: DataTree<*>?,
        routeMeta: Meta,
    ): SiteBuilder = KtorSiteBuilder(
        snark = snark,
        data = dataOverride ?: data,
        siteMeta = Laminate(routeMeta, siteMeta),
        baseUrl = resolveRef(baseUrl, routeName.toWebPath()),
        route = Name.EMPTY,
        ktorRoute = ktorRoute.createRouteFromPath(routeName.toWebPath())
    )

//
//    override fun resourceFile(resourcesPath: String, webPath: String) {
//        ktorRoute.resource(resourcesPath, resourcesPath)
//    }

//    override fun resourceDirectory(resourcesPath: String) {
//        ktorRoute.resources(resourcesPath)
//    }
}

private fun Route.site(
    snarkHtmlPlugin: SnarkHtml,
    data: DataTree<*>,
    baseUrl: String = "",
    siteMeta: Meta = data.meta,
    block: KtorSiteBuilder.() -> Unit,
) {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    block(KtorSiteBuilder(snarkHtmlPlugin, data, siteMeta, baseUrl, route = Name.EMPTY, this@Route))
}

public fun Application.site(
    snark: SnarkHtml,
    data: DataTree<*>,
    baseUrl: String = "",
    siteMeta: Meta = data.meta,
    block: SiteBuilder.() -> Unit,
) {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    routing {
        site(snark, data, baseUrl, siteMeta, block)
    }
}
