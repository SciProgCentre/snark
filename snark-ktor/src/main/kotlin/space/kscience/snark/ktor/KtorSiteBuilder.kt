package space.kscience.snark.ktor

import io.ktor.http.ContentType
import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import io.ktor.http.fromFileExtension
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.html.respondHtml
import io.ktor.server.http.content.file
import io.ktor.server.http.content.files
import io.ktor.server.http.content.static
import io.ktor.server.plugins.origin
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.Route
import io.ktor.server.routing.createRouteFromPath
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.css.CssBuilder
import kotlinx.html.CommonAttributeGroupFacade
import kotlinx.html.HTML
import kotlinx.html.style
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
import space.kscience.snark.SnarkEnvironment
import space.kscience.snark.html.*
import java.nio.file.Path
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.reflect.typeOf

public fun CommonAttributeGroupFacade.css(block: CssBuilder.() -> Unit) {
    style = CssBuilder().block().toString()
}

public class KtorSiteBuilder(
    override val snark: SnarkHtmlPlugin,
    override val data: DataTree<*>,
    override val siteMeta: Meta,
    private val baseUrl: String,
    override val route: Name,
    private val ktorRoute: Route,
) : SiteBuilder {

    private fun file(item: DataTreeItem<Any>, routeName: Name) {
        val extension = item.meta[FileData.FILE_EXTENSION_KEY]?.string?.let { ".$it" } ?: ""

        //try using direct file rendering
        item.meta[FileData.FILE_PATH_KEY]?.string?.let {
            try {
                val file = Path.of(it).toFile()
                if (file.isDirectory) {
                    ktorRoute.static(routeName.toWebPath()) {
                        files(file)
                    }
                } else {
                    val fileName = routeName.toWebPath() + extension //TODO add extension
                    ktorRoute.file(fileName, file)
                }
                //success, don't do anything else
                return@file
            } catch (ex: Exception) {
                //failure,
                return@let
            }
        }
        when (item) {
            is DataTreeItem.Leaf -> {
                val datum = item.data
                if (datum.type != typeOf<Binary>()) error("Can't directly serve file of type ${item.data.type}")
                ktorRoute.get(routeName.toWebPath() + extension) {
                    val binary = datum.await() as Binary
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
                    file(childItem, routeName + token)
                }
            }
        }
    }

    override fun file(dataName: Name, routeName: Name) {
        val item: DataTreeItem<Any> = data.getItem(dataName) ?: error("Data with name is not resolved")
        file(item, routeName)
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
        override val snark: SnarkHtmlPlugin get() = this@KtorSiteBuilder.snark
        override val data: DataTree<*> get() = this@KtorSiteBuilder.data

        override fun resolveRef(ref: String): String = resolveRef(pageBaseUrl, ref)

        override fun resolvePageRef(
            pageName: Name,
            relative: Boolean,
        ): String {
            val fullPageName = if (relative) route + pageName else pageName
            return if (fullPageName.endsWith(SiteBuilder.INDEX_PAGE_TOKEN)) {
                resolveRef(fullPageName.cutLast().toWebPath())
            } else {
                resolveRef(fullPageName.toWebPath())
            }
        }
    }

    override fun page(route: Name, pageMeta: Meta, content: context(WebPage, HTML)() -> Unit) {
        ktorRoute.get(route.toWebPath()) {
            call.respondHtml {
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
                content(pageBuilder, this)
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

context(Route, SnarkEnvironment)
private fun siteInRoute(
    baseUrl: String = "",
    block: KtorSiteBuilder.() -> Unit,
) {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    block(KtorSiteBuilder(buildHtmlPlugin(), data, meta, baseUrl, route = Name.EMPTY, this@Route))
}

context(Application)
public fun SnarkEnvironment.site(
    baseUrl: String = "",
    block: KtorSiteBuilder.() -> Unit,
) {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    routing {
        siteInRoute(baseUrl, block)
    }
}
