package space.kscience.snark.html.static

import kotlinx.html.html
import kotlinx.html.stream.createHTML
import kotlinx.io.asSink
import kotlinx.io.buffered
import space.kscience.dataforge.data.*
import space.kscience.dataforge.io.Binary
import space.kscience.dataforge.io.writeBinary
import space.kscience.dataforge.meta.*
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.isEmpty
import space.kscience.dataforge.names.plus
import space.kscience.dataforge.workspace.FileData
import space.kscience.snark.html.*
import java.nio.file.Path
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.io.path.*
import kotlin.reflect.typeOf


/**
 * An implementation of [SiteContext] to render site as a static directory [outputPath]
 */
internal class StaticSiteContext(
    override val siteMeta: Meta,
    private val baseUrl: String,
    override val route: Name,
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
    override suspend fun static(route: Name, data: Data<Binary>) {
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
        val binary = data.await()
        targetPath.outputStream().asSink().buffered().use {
            it.writeBinary(binary)
        }
    }

    private fun resolveRef(baseUrl: String, ref: String) = if (baseUrl.isEmpty()) {
        ref
    } else if (ref.isEmpty()) {
        baseUrl
    } else {
        "${baseUrl.removeSuffix("/")}/$ref"
    }

    class StaticPageContext(override val site: StaticSiteContext, override val pageMeta: Meta) : PageContext {

        override fun resolveRef(ref: String): String =
            site.resolveRef(site.baseUrl, ref)

        override fun resolvePageRef(pageName: Name, relative: Boolean): String = resolveRef(
            (if (relative) site.route + pageName else pageName).toWebPath() + ".html"
        )
    }

    override suspend fun page(route: Name, data: DataSet<Any>, pageMeta: Meta, htmlPage: HtmlPage) {


        val modifiedPageMeta = pageMeta.toMutableMeta().apply {
            "name" put route.toString()
        }

        val newPath = if (route.isEmpty()) {
            outputPath.resolve("index.html")
        } else {
            outputPath.resolve(route.toWebPath() + ".html")
        }

        newPath.parent.createDirectories()

        val pageContext = StaticPageContext(this, Laminate(modifiedPageMeta, siteMeta))
        newPath.writeText(HtmlPage.createHtmlString(pageContext,htmlPage, data))
    }

    override suspend fun site(route: Name, data: DataSet<Any>, siteMeta: Meta, htmlSite: HtmlSite) {
        with(htmlSite) {
            StaticSiteContext(
                siteMeta = Laminate(siteMeta, this@StaticSiteContext.siteMeta),
                baseUrl = if (baseUrl == "") "" else resolveRef(baseUrl, route.toWebPath()),
                route = Name.EMPTY,
                outputPath = outputPath.resolve(route.toWebPath())
            ).renderSite(data)
        }
    }

}

/**
 * Create a static site using given [SnarkEnvironment] in provided [outputPath].
 * Use [siteUrl] as a base for all resolved URLs. By default, use [outputPath] absolute path as a base.
 *
 */
public fun SnarkHtml.staticSite(
    data: DataSet<*>,
    outputPath: Path,
    siteUrl: String = outputPath.absolutePathString().replace("\\", "/"),
    siteMeta: Meta = data.meta,
    block: SiteContext.() -> Unit,
) {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    StaticSiteContext(siteMeta, siteUrl, Name.EMPTY, outputPath).block()
}