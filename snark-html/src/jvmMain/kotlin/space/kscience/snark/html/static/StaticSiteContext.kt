package space.kscience.snark.html.static

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.io.asSink
import kotlinx.io.buffered
import space.kscience.dataforge.context.Context
import space.kscience.dataforge.data.Data
import space.kscience.dataforge.data.DataTree
import space.kscience.dataforge.data.await
import space.kscience.dataforge.data.meta
import space.kscience.dataforge.io.Binary
import space.kscience.dataforge.io.writeBinary
import space.kscience.dataforge.meta.Laminate
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.string
import space.kscience.dataforge.meta.toMutableMeta
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.isEmpty
import space.kscience.dataforge.workspace.FileDataTree
import space.kscience.snark.html.*
import java.nio.file.Path
import kotlin.io.path.*
import kotlin.reflect.typeOf


/**
 * An implementation of [SiteContext] to render site as a static directory [outputPath]
 */
internal class StaticSiteContext(
    override val context: Context,
    override val siteMeta: Meta,
    private val baseUri: String,
    override val path: List<String>,
    override val siteRoute: Name,
    override val parent: SiteContext?,
    private val outputPath: Path,
) : SiteContext {

    @OptIn(ExperimentalPathApi::class)
    override fun static(route: Name, data: Data<Binary>) {
        //if data is a file, copy it
        data.meta[FileDataTree.FILE_PATH_KEY]?.string?.let {
            val file = Path.of(it)
            val targetPath = outputPath.resolve(route.toWebPath())
            targetPath.parent.createDirectories()
            file.copyToRecursively(targetPath, followLinks = false, overwrite = true)
            //success, don't do anything else
            return
        }

        if (data.type != typeOf<Binary>()) error("Can't directly serve file of type ${data.type}")
        val targetPath = outputPath.resolve(route.toWebPath())
        runBlocking(Dispatchers.IO) {
            val binary = data.await()
            targetPath.outputStream().asSink().buffered().use {
                it.writeBinary(binary)
            }
        }
    }

//    private fun resolveRef(baseUrl: String, ref: String) = if (baseUrl.isEmpty()) {
//        ref
//    } else if (ref.isEmpty()) {
//        baseUrl
//    } else {
//        "${baseUrl.removeSuffix("/")}/$ref"
//    }

    class StaticPageContext(
        override val site: StaticSiteContext,
        override val hostUri: String,
        override val pageRoute: Name,
        override val pageMeta: Meta,
    ) : PageContext {

        override fun resolvePageRef(pageName: Name, targetSite: SiteContext): String = resolveRef(
            pageName.toWebPath() + ".html",
            targetSite
        )
    }

    override fun page(route: Name, data: DataTree<*>?, pageMeta: Meta, content: HtmlPage) {
        val modifiedPageMeta = pageMeta.toMutableMeta().apply {
            "name" put route.toString()
        }

        val newPath = if (route.isEmpty()) {
            outputPath.resolve("index.html")
        } else {
            outputPath.resolve(route.toWebPath() + ".html")
        }

        newPath.parent.createDirectories()

        val pageContext = StaticPageContext(this, baseUri, route, Laminate(modifiedPageMeta, siteMeta))
        newPath.writeText(HtmlPage.createHtmlString(pageContext, data, content))
    }

    override fun route(route: Name, data: DataTree<*>?, siteMeta: Meta, content: HtmlSite) {
        val siteContextWithData = SiteContextWithData(
            StaticSiteContext(
                context = context,
                siteMeta = Laminate(siteMeta, this@StaticSiteContext.siteMeta),
                baseUri = baseUri,
                path = emptyList(),
                siteRoute = route,
                parent = parent,
                outputPath = outputPath.resolve(route.toWebPath())
            ),
            data ?: DataTree.EMPTY
        )
        with(content) {
            with(siteContextWithData) {
                renderSite()
            }
        }
    }

    override fun site(route: Name, data: DataTree<*>?, siteMeta: Meta, content: HtmlSite) {
        val siteContextWithData = SiteContextWithData(
            StaticSiteContext(
                context = context,
                siteMeta = Laminate(siteMeta, this@StaticSiteContext.siteMeta),
                baseUri = baseUri,
                path = emptyList(),
                siteRoute = route,
                parent = this,
                outputPath = outputPath.resolve(route.toWebPath())
            ),
            data ?: DataTree.EMPTY
        )
        with(content) {
            with(siteContextWithData) {
                renderSite()
            }
        }
    }

}

/**
 * Create a static site using given [content] in provided [outputPath].
 * Use [siteUri] as a base for all resolved URLs. By default, use [outputPath] absolute path as a base.
 *
 */
public suspend fun SnarkHtml.staticSite(
    data: DataTree<*>?,
    outputPath: Path,
    siteUri: String = outputPath.absolute().toUri().toString(),
    siteMeta: Meta = data?.meta ?: Meta.EMPTY,
    content: HtmlSite,
) {
    val siteContextWithData = SiteContextWithData(
        StaticSiteContext(context, siteMeta, siteUri, emptyList(), Name.EMPTY, null, outputPath),
        data ?: DataTree.EMPTY
    )
    with(content) {
        with(siteContextWithData) {
            renderSite()
        }
    }
}