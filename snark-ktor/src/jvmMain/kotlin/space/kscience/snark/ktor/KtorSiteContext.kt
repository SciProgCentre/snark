package space.kscience.snark.ktor

import io.ktor.http.*
import io.ktor.http.content.TextContent
import io.ktor.server.http.content.staticFiles
import io.ktor.server.plugins.origin
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.Route
import io.ktor.server.routing.createRouteFromPath
import io.ktor.server.routing.get
import space.kscience.dataforge.context.Context
import space.kscience.dataforge.context.ContextAware
import space.kscience.dataforge.context.error
import space.kscience.dataforge.context.logger
import space.kscience.dataforge.data.Data
import space.kscience.dataforge.data.DataTree
import space.kscience.dataforge.data.await
import space.kscience.dataforge.data.meta
import space.kscience.dataforge.io.Binary
import space.kscience.dataforge.io.toByteArray
import space.kscience.dataforge.meta.*
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.cutLast
import space.kscience.dataforge.names.endsWith
import space.kscience.dataforge.names.plus
import space.kscience.dataforge.workspace.FileDataTree
import space.kscience.snark.html.*
import java.nio.file.Path
import kotlin.reflect.typeOf

//public fun CommonAttributeGroupFacade.css(block: CssBuilder.() -> Unit) {
//    style = CssBuilder().block().toString()
//}

/**
 * Ktor-based implementation of [SiteContext] that maps site declarations to Ktor routes.
 *
 * This context is used while rendering a site into a running Ktor routing tree. Pages are
 * registered as HTTP GET handlers, static binary data is exposed either from a filesystem path
 * when file metadata is available or directly from in-memory binary data, and nested route or
 * site blocks create child contexts backed by child Ktor routes.
 *
 * Page rendering is performed inside the Ktor call coroutine so that page content can access
 * request-derived information such as the host URI. Page metadata is combined with site metadata
 * and augmented with routing information before being passed to the page renderer.
 *
 * The context preserves the logical site hierarchy through its parent context, route name, and
 * path information while delegating actual HTTP endpoint registration to the underlying Ktor
 * [Route].
 */
internal class KtorSiteContext(
    override val context: Context,
    override val siteMeta: Meta,
    override val path: List<String>,
    override val siteRoute: Name,
    override val parent: SiteContext?,
    private val ktorRoute: Route,
) : SiteContext, ContextAware {


    override fun static(route: Name, data: Data<Binary>) {
        data.meta[FileDataTree.FILE_PATH_KEY]?.string?.let {
            val file = try {
                Path.of(it).toFile()
            } catch (ex: Exception) {
                //failure,
                logger.error { "File $it could not be converted to java.io.File" }
                return@let
            }

            val fileName = route.toWebPath()
            ktorRoute.staticFiles(fileName, file)
            //success, don't do anything else
            return
        }

        if (data.type != typeOf<Binary>()) error("Can't directly serve file of type ${data.type}")
        ktorRoute.get(route.toWebPath()) {
            val binary = data.await()
            val extension = data.meta[FileDataTree.FILE_EXTENSION_KEY]?.string?.let { ".$it" } ?: ""
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


    private class KtorPageContext(
        override val site: KtorSiteContext,
        override val hostUri: String,
        override val pageRoute: Name,
        override val pageMeta: Meta,
    ) : PageContext {

        override fun resolvePageRef(
            pageName: Name,
            targetSite: SiteContext,
        ): String {
            return if (pageName.endsWith(SiteContext.INDEX_PAGE_TOKEN)) {
                resolveRef(pageName.cutLast().toWebPath(), targetSite)
            } else {
                resolveRef(pageName.toWebPath(), targetSite)
            }
        }
    }

    override fun page(route: Name, data: DataTree<*>?, pageMeta: Meta, content: HtmlPage) {
        ktorRoute.get(route.toWebPath()) {
            val request = call.request
            //substitute host for url for backwards calls
//            val url = URLBuilder(baseUrl).apply {
//                protocol = URLProtocol.createOrDefault(request.origin.scheme)
//                host = request.origin.serverHost
//                port = request.origin.serverPort
//            }

            val hostUrl = URLBuilder().apply {
                protocol = URLProtocol.createOrDefault(request.origin.scheme)
                host = request.origin.serverHost
                port = request.origin.serverPort
            }

            val modifiedPageMeta = pageMeta.copy {
                "host" put hostUrl.buildString()
                "path" put path.map { it.asValue() }.asValue()
                "route" put (siteRoute + route).toString()
            }

            val pageContext = KtorPageContext(
                site = this@KtorSiteContext,
                hostUri = hostUrl.buildString(),
                pageRoute = siteRoute + route,
                pageMeta = Laminate(modifiedPageMeta, siteMeta)
            )
            //render page in suspend environment
            val html = HtmlPage.createHtmlString(pageContext, data, content)

            call.respond(TextContent(html, ContentType.Text.Html.withCharset(Charsets.UTF_8), HttpStatusCode.OK))
        }
    }

    public fun alias(route: Name, target: Name) {
        ktorRoute.get(route.toWebPath()) {
            call.respondRedirect(target.toWebPath())
        }
    }

    override fun route(route: Name, data: DataTree<*>?, siteMeta: Meta, content: HtmlSite) {
        val siteContext = SiteContextWithData(
            KtorSiteContext(
                context,
                siteMeta = Laminate(siteMeta, this@KtorSiteContext.siteMeta),
                path = path,
                siteRoute = route,
                parent,
                ktorRoute = ktorRoute.createRouteFromPath(route.toWebPath())
            ),
            data ?: DataTree.EMPTY
        )
        with(content) {
            with(siteContext) {
                renderSite()
            }
        }
    }

    override fun site(route: Name, data: DataTree<*>?, siteMeta: Meta, content: HtmlSite) {
        val siteContext = SiteContextWithData(
            KtorSiteContext(
                context,
                siteMeta = Laminate(siteMeta, this@KtorSiteContext.siteMeta),
                path = path + route.tokens.map { it.toStringUnescaped() },
                siteRoute = Name.EMPTY,
                this,
                ktorRoute = ktorRoute.createRouteFromPath(route.toWebPath())
            ),
            data ?: DataTree.EMPTY
        )
        context(siteContext) {
            content.renderSite()
        }
    }

}

public fun Route.site(
    context: Context,
    data: DataTree<*>?,
    path: List<String> = emptyList(),
    siteMeta: Meta = data?.meta ?: Meta.EMPTY,
    content: HtmlSite,
) {
    val siteContext = SiteContextWithData(
        KtorSiteContext(context, siteMeta, path = path, siteRoute = Name.EMPTY, null, this),
        data ?: DataTree.EMPTY
    )
    context(siteContext) {
        content.renderSite()
    }
}
//
//public suspend fun Application.site(
//    context: Context,
//    data: DataSet<*>,
//    baseUrl: String = "",
//    siteMeta: Meta = data.meta,
//    content: HtmlSite,
//) {
//    routing {}.site(context, data, baseUrl, siteMeta, content)
//
//}
