package space.kscience.snark.ktor

import io.ktor.http.*
import io.ktor.http.content.TextContent
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.http.content.staticFiles
import io.ktor.server.plugins.origin
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.Route
import io.ktor.server.routing.createRouteFromPath
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import space.kscience.dataforge.context.Context
import space.kscience.dataforge.context.ContextAware
import space.kscience.dataforge.context.error
import space.kscience.dataforge.context.logger
import space.kscience.dataforge.data.Data
import space.kscience.dataforge.data.DataSet
import space.kscience.dataforge.data.DataTree
import space.kscience.dataforge.data.await
import space.kscience.dataforge.io.Binary
import space.kscience.dataforge.io.toByteArray
import space.kscience.dataforge.meta.Laminate
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.string
import space.kscience.dataforge.meta.toMutableMeta
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.cutLast
import space.kscience.dataforge.names.endsWith
import space.kscience.dataforge.names.plus
import space.kscience.dataforge.workspace.FileData
import space.kscience.snark.html.*
import java.nio.file.Path
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.reflect.typeOf

//public fun CommonAttributeGroupFacade.css(block: CssBuilder.() -> Unit) {
//    style = CssBuilder().block().toString()
//}

public class KtorSiteContext(
    override val context: Context,
    override val siteMeta: Meta,
    private val baseUrl: String,
    override val route: Name,
    private val ktorRoute: Route,
) : SiteContext, ContextAware {


    override suspend fun static(route: Name, data: Data<Binary>) {
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

    private fun resolveRef(baseUrl: String, ref: String) = if (baseUrl.isEmpty()) {
        ref
    } else if (ref.isEmpty()) {
        baseUrl
    } else {
        "${baseUrl.removeSuffix("/")}/$ref"
    }


    private class KtorPageContext(
        override val site: KtorSiteContext,
        val pageBaseUrl: String,
        override val pageMeta: Meta,
    ) : PageContext {

        override fun resolveRef(ref: String): String = site.resolveRef(pageBaseUrl, ref)

        override fun resolvePageRef(
            pageName: Name,
            relative: Boolean,
        ): String {
            val fullPageName = if (relative) site.route + pageName else pageName
            return if (fullPageName.endsWith(SiteContext.INDEX_PAGE_TOKEN)) {
                resolveRef(fullPageName.cutLast().toWebPath())
            } else {
                resolveRef(fullPageName.toWebPath())
            }
        }
    }

    override suspend fun page(route: Name, data: DataSet<Any>, pageMeta: Meta, htmlPage: HtmlPage) {
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
            val pageContext =
                KtorPageContext(this@KtorSiteContext, url.buildString(), Laminate(modifiedPageMeta, siteMeta))
            //render page in suspend environment
            val html = HtmlPage.createHtmlString(pageContext, htmlPage, data)

            call.respond(TextContent(html, ContentType.Text.Html.withCharset(Charsets.UTF_8), HttpStatusCode.OK))
        }
    }

    override suspend fun site(route: Name, data: DataSet<Any>, siteMeta: Meta, htmlSite: HtmlSite) {
        with(htmlSite) {
            KtorSiteContext(
                context,
                siteMeta = Laminate(siteMeta, this@KtorSiteContext.siteMeta),
                baseUrl = resolveRef(baseUrl, route.toWebPath()),
                route = Name.EMPTY,
                ktorRoute = ktorRoute.createRouteFromPath(route.toWebPath())
            ).renderSite(data)
        }
    }

}

private fun Route.site(
    context: Context,
    data: DataTree<*>,
    baseUrl: String = "",
    siteMeta: Meta = data.meta,
    block: KtorSiteContext.() -> Unit,
) {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    block(KtorSiteContext(context, siteMeta, baseUrl, route = Name.EMPTY, this@Route))
}

public fun Application.site(
    context: Context,
    data: DataTree<*>,
    baseUrl: String = "",
    siteMeta: Meta = data.meta,
    block: SiteContext.() -> Unit,
) {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    routing {
        site(context, data, baseUrl, siteMeta, block)
    }
}
