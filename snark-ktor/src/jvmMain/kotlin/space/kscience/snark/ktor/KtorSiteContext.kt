package space.kscience.snark.ktor

import io.ktor.http.*
import io.ktor.http.content.TextContent
import io.ktor.server.application.call
import io.ktor.server.http.content.staticFiles
import io.ktor.server.plugins.origin
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
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
import space.kscience.dataforge.workspace.FileData
import space.kscience.snark.html.*
import java.nio.file.Path
import kotlin.reflect.typeOf

//public fun CommonAttributeGroupFacade.css(block: CssBuilder.() -> Unit) {
//    style = CssBuilder().block().toString()
//}

internal class KtorSiteContext(
    override val context: Context,
    override val siteMeta: Meta,
    override val path: List<String>,
    override val siteRoute: Name,
    private val ktorRoute: Route,
) : SiteContext, ContextAware {


    override fun static(route: Name, data: Data<Binary>) {
        data.meta[FileData.FILE_PATH_KEY]?.string?.let {
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
            val extension = data.meta[FileData.FILE_EXTENSION_KEY]?.string?.let { ".$it" } ?: ""
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
        override val host: Url,
        override val pageRoute: Name,
        override val pageMeta: Meta,
    ) : PageContext {

        override fun resolvePageRef(
            pageName: Name,
        ): String {
            return if (pageName.endsWith(SiteContext.INDEX_PAGE_TOKEN)) {
                resolveRef(pageName.cutLast().toWebPath())
            } else {
                resolveRef(pageName.toWebPath())
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
                host = hostUrl.build(),
                pageRoute = siteRoute + route,
                pageMeta = Laminate(modifiedPageMeta, siteMeta)
            )
            //render page in suspend environment
            val html = HtmlPage.createHtmlString(pageContext, data, content)

            call.respond(TextContent(html, ContentType.Text.Html.withCharset(Charsets.UTF_8), HttpStatusCode.OK))
        }
    }

    override fun route(route: Name, data: DataTree<*>?, siteMeta: Meta, content: HtmlSite) {
        val siteContext = SiteContextWithData(
            KtorSiteContext(
                context,
                siteMeta = Laminate(siteMeta, this@KtorSiteContext.siteMeta),
                path = path,
                siteRoute = route,
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

}

public fun Route.site(
    context: Context,
    data: DataTree<*>?,
    path: List<String> = emptyList(),
    siteMeta: Meta = data?.meta ?: Meta.EMPTY,
    content: HtmlSite,
) {
    val siteContext = SiteContextWithData(
        KtorSiteContext(context, siteMeta, path = path, siteRoute = Name.EMPTY, this@Route),
        data ?: DataTree.EMPTY
    )
    with(content) {
        with(siteContext) {
            renderSite()
        }
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
