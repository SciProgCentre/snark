package space.kscience.snark.ktor

import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.html.respondHtml
import io.ktor.server.http.content.*
import io.ktor.server.plugins.origin
import io.ktor.server.request.host
import io.ktor.server.request.port
import io.ktor.server.routing.Route
import io.ktor.server.routing.createRouteFromPath
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.html.HTML
import space.kscience.dataforge.data.DataTree
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.withDefault
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.cutLast
import space.kscience.dataforge.names.endsWith
import space.kscience.snark.html.*
import java.nio.file.Path
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@PublishedApi
internal class KtorSiteBuilder(
    override val snark: SnarkPlugin,
    override val data: DataTree<*>,
    override val siteMeta: Meta,
    private val baseUrl: String,
    private val ktorRoute: Route,
) : SiteBuilder {

    override fun assetFile(remotePath: String, file: Path) {
        ktorRoute.file(remotePath, file.toFile())
    }

    override fun assetDirectory(remotePath: String, directory: Path) {
        ktorRoute.static(remotePath) {
            files(directory.toFile())
        }
    }

    private fun resolveRef(baseUrl: String, ref: String) = if (baseUrl.isEmpty()) {
        ref
    } else if (ref.isEmpty()) {
        baseUrl
    } else {
        "${baseUrl.removeSuffix("/")}/$ref"
    }


    inner class KtorPage(
        val pageBaseUrl: String,
        override val pageMeta: Meta = this@KtorSiteBuilder.siteMeta,
    ) : Page {
        override val snark: SnarkPlugin get() = this@KtorSiteBuilder.snark
        override val data: DataTree<*> get() = this@KtorSiteBuilder.data

        override fun resolveRef(ref: String): String = resolveRef(pageBaseUrl, ref)

        override fun resolvePageRef(pageName: Name): String = if (pageName.endsWith(SiteBuilder.INDEX_PAGE_TOKEN)) {
            resolveRef(pageName.cutLast().toWebPath())
        } else {
            resolveRef(pageName.toWebPath())
        }
    }

    override fun page(route: Name, content: context(Page, HTML)() -> Unit) {
        ktorRoute.get(route.toWebPath()) {
            call.respondHtml {
                val request = call.request
                //substitute host for url for backwards calls
                val url = URLBuilder(baseUrl).apply {
                    protocol = URLProtocol.createOrDefault(request.origin.scheme)
                    host = request.host()
                    port = request.port()
                }
                val pageBuilder = KtorPage(url.buildString())
                content(pageBuilder, this)
            }
        }
    }

    override fun route(
        routeName: Name,
        dataOverride: DataTree<*>?,
        metaOverride: Meta?,
        setAsRoot: Boolean,
    ): SiteBuilder = KtorSiteBuilder(
        snark = snark,
        data = dataOverride ?: data,
        siteMeta = metaOverride?.withDefault(siteMeta) ?: siteMeta,
        baseUrl = if (setAsRoot) {
            resolveRef(baseUrl, routeName.toWebPath())
        } else {
            baseUrl
        },
        ktorRoute = ktorRoute.createRouteFromPath(routeName.toWebPath())
    )


    override fun assetResourceFile(remotePath: String, resourcesPath: String) {
        ktorRoute.resource(resourcesPath, resourcesPath)
    }

    override fun assetResourceDirectory(resourcesPath: String) {
        ktorRoute.resources(resourcesPath)
    }
}

public inline fun Route.snarkSite(
    snark: SnarkPlugin,
    data: DataTree<*>,
    meta: Meta = data.meta,
    block: SiteBuilder.() -> Unit,
) {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    block(KtorSiteBuilder(snark, data, meta, "", this@snarkSite))
}

public fun Application.snarkSite(
    snark: SnarkPlugin,
    data: DataTree<*> = DataTree.empty(),
    meta: Meta = data.meta,
    block: SiteBuilder.() -> Unit,
) {
    routing {
        snarkSite(snark, data, meta, block)
    }
}