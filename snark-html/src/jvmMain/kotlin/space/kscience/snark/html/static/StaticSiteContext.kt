package space.kscience.snark.html.static

import io.ktor.http.Url
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.io.asSink
import kotlinx.io.buffered
import space.kscience.dataforge.data.*
import space.kscience.dataforge.io.Binary
import space.kscience.dataforge.io.writeBinary
import space.kscience.dataforge.meta.*
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.isEmpty
import space.kscience.dataforge.workspace.FileData
import space.kscience.snark.html.*
import java.nio.file.Path
import kotlin.io.path.*
import kotlin.reflect.typeOf


/**
 * An implementation of [SiteContext] to render site as a static directory [outputPath]
 */
internal class StaticSiteContext(
    override val siteMeta: Meta,
    private val baseUrl: Url,
    override val path: List<String>,
    override val siteRoute: Name,
    private val outputPath: Path,
) : SiteContext {


//    @OptIn(ExperimentalPathApi::class)
//    private suspend fun files(item: DataTreeItem<Any>, routeName: Name) {
//        //try using direct file rendering
//        item.meta[FileData.FILE_PATH_KEY]?.string?.let {
//            val file = Path.of(it)
//            val targetPath = outputPath.resolve(routeName.toWebPath())
//            targetPath.parent.createDirectories()
//            file.copyToRecursively(targetPath, followLinks = false)
//            //success, don't do anything else
//            return@files
//        }
//
//        when (item) {
//            is DataTreeItem.Leaf -> {
//                val datum = item.data
//                if (datum.type != typeOf<Binary>()) error("Can't directly serve file of type ${item.data.type}")
//                val targetPath = outputPath.resolve(routeName.toWebPath())
//                val binary = datum.await() as Binary
//                targetPath.outputStream().asSink().buffered().use {
//                    it.writeBinary(binary)
//                }
//            }
//
//            is DataTreeItem.Node -> {
//                item.tree.items.forEach { (token, childItem) ->
//                    files(childItem, routeName + token)
//                }
//            }
//        }
//    }

    @OptIn(ExperimentalPathApi::class)
    override fun static(route: Name, data: Data<Binary>) {
        //if data is a file, copy it
        data.meta[FileData.FILE_PATH_KEY]?.string?.let {
            val file = Path.of(it)
            val targetPath = outputPath.resolve(route.toWebPath())
            targetPath.parent.createDirectories()
            file.copyToRecursively(targetPath, followLinks = false)
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
        override val host: Url,
        override val pageRoute: Name,
        override val pageMeta: Meta,
    ) : PageContext {

        override fun resolvePageRef(pageName: Name): String = resolveRef(
            pageName.toWebPath() + ".html"
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

        val pageContext = StaticPageContext(this, baseUrl, route, Laminate(modifiedPageMeta, siteMeta))
        newPath.writeText(HtmlPage.createHtmlString(pageContext, data, content))
    }

    override fun route(route: Name, data: DataTree<*>?, siteMeta: Meta, content: HtmlSite) {
        val siteContextWithData = SiteContextWithData(
            StaticSiteContext(
                siteMeta = Laminate(siteMeta, this@StaticSiteContext.siteMeta),
                baseUrl = baseUrl,
                path = emptyList(),
                siteRoute = route,
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
                siteMeta = Laminate(siteMeta, this@StaticSiteContext.siteMeta),
                baseUrl = baseUrl,
                path = emptyList(),
                siteRoute = route,
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
 * Create a static site using given [SnarkEnvironment] in provided [outputPath].
 * Use [siteUrl] as a base for all resolved URLs. By default, use [outputPath] absolute path as a base.
 *
 */
@Suppress("UnusedReceiverParameter")
public suspend fun SnarkHtml.staticSite(
    data: DataTree<*>?,
    outputPath: Path,
    siteUrl: Url = Url(outputPath.absolutePathString()),
    siteMeta: Meta = data?.meta ?: Meta.EMPTY,
    content: HtmlSite,
) {
    val siteContextWithData = SiteContextWithData(
        StaticSiteContext(siteMeta, siteUrl, emptyList(), Name.EMPTY, outputPath),
        data ?: DataTree.EMPTY
    )
    with(content) {
        with(siteContextWithData) {
            renderSite()
        }
    }
}