package space.kscience.snark.html

import kotlinx.html.HTML
import kotlinx.html.html
import kotlinx.html.stream.createHTML
import space.kscience.dataforge.data.DataTree
import space.kscience.dataforge.meta.Laminate
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.toMutableMeta
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.isEmpty
import space.kscience.dataforge.names.plus
import java.nio.file.Files
import java.nio.file.Path
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.io.path.*


/**
 * An implementation of [SiteBuilder] to render site as a static directory [outputPath]
 */
internal class StaticSiteBuilder(
    override val snark: SnarkHtmlPlugin,
    override val data: DataTree<*>,
    override val siteMeta: Meta,
    private val baseUrl: String,
    override val route: Name,
    private val outputPath: Path,
) : SiteBuilder {

    override fun static(dataName: Name, routeName: Name) {
        TODO("Not yet implemented")
    }

    private fun Path.copyRecursively(target: Path) {
        Files.walk(this).forEach { source: Path ->
            val destination: Path = target.resolve(source.relativeTo(this))
            if (!destination.isDirectory()) {
                //avoid re-creating directories
                source.copyTo(destination, true)
            }
        }
    }
//
//    override fun file(file: Path, webPath: String) {
//        val targetPath = outputPath.resolve(webPath)
//        if (file.isDirectory()) {
//            targetPath.parent.createDirectories()
//            file.copyRecursively(targetPath)
//        } else if (webPath.isBlank()) {
//            error("Can't mount file to an empty route")
//        } else {
//            targetPath.parent.createDirectories()
//            file.copyTo(targetPath, true)
//        }
//    }
//
//    override fun resourceFile(resourcesPath: String, webPath: String) {
//        val targetPath = outputPath.resolve(webPath)
//        targetPath.parent.createDirectories()
//        javaClass.getResource(resourcesPath)?.let { Path.of(it.toURI()) }?.copyTo(targetPath, true)
//    }
//
//    override fun resourceDirectory(resourcesPath: String) {
//        outputPath.parent.createDirectories()
//        javaClass.getResource(resourcesPath)?.let { Path.of(it.toURI()) }?.copyRecursively(outputPath)
//    }

    private fun resolveRef(baseUrl: String, ref: String) = if (baseUrl.isEmpty()) {
        ref
    } else if (ref.isEmpty()) {
        baseUrl
    } else {
        "${baseUrl.removeSuffix("/")}/$ref"
    }

    inner class StaticWebPage(override val pageMeta: Meta) : WebPage {
        override val data: DataTree<*> get() = this@StaticSiteBuilder.data

        override val snark: SnarkHtmlPlugin get() = this@StaticSiteBuilder.snark


        override fun resolveRef(ref: String): String =
            this@StaticSiteBuilder.resolveRef(this@StaticSiteBuilder.baseUrl, ref)

        override fun resolvePageRef(pageName: Name, relative: Boolean): String = resolveRef(
            (if (relative) this@StaticSiteBuilder.route + pageName else pageName).toWebPath() + ".html"
        )
    }


    override fun page(route: Name, pageMeta: Meta, content: context(HTML) WebPage.() -> Unit) {
        val htmlBuilder = createHTML()

        val modifiedPageMeta = pageMeta.toMutableMeta().apply {
            "name" put route.toString()
        }

        htmlBuilder.html {
            content(this, StaticWebPage(Laminate(modifiedPageMeta, siteMeta)))
        }

        val newPath = if (route.isEmpty()) {
            outputPath.resolve("index.html")
        } else {
            outputPath.resolve(route.toWebPath() + ".html")
        }

        newPath.parent.createDirectories()
        newPath.writeText(htmlBuilder.finalize())
    }

    override fun route(
        routeName: Name,
        dataOverride: DataTree<*>?,
        routeMeta: Meta,
    ): SiteBuilder = StaticSiteBuilder(
        snark = snark,
        data = dataOverride ?: data,
        siteMeta = Laminate(routeMeta, siteMeta),
        baseUrl = baseUrl,
        route = route + routeName,
        outputPath = outputPath.resolve(routeName.toWebPath())
    )

    override fun site(
        routeName: Name,
        dataOverride: DataTree<*>?,
        routeMeta: Meta,
    ): SiteBuilder = StaticSiteBuilder(
        snark = snark,
        data = dataOverride ?: data,
        siteMeta = Laminate(routeMeta, siteMeta),
        baseUrl = if (baseUrl == "") "" else resolveRef(baseUrl, routeName.toWebPath()),
        route = Name.EMPTY,
        outputPath = outputPath.resolve(routeName.toWebPath())
    )
}

/**
 * Create a static site using given [SnarkEnvironment] in provided [outputPath].
 * Use [siteUrl] as a base for all resolved URLs. By default, use [outputPath] absolute path as a base.
 *
 */
public fun SnarkHtmlPlugin.static(
    data: DataTree<*>,
    outputPath: Path,
    siteUrl: String = outputPath.absolutePathString().replace("\\", "/"),
    siteMeta: Meta = data.meta,
    block: SiteBuilder.() -> Unit,
) {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    StaticSiteBuilder(this, data, siteMeta, siteUrl, Name.EMPTY, outputPath).block()
}