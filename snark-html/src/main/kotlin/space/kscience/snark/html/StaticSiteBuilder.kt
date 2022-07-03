package space.kscience.snark.html

import kotlinx.html.HTML
import kotlinx.html.html
import kotlinx.html.stream.createHTML
import space.kscience.dataforge.data.DataTree
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.withDefault
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.isEmpty
import space.kscience.snark.SnarkEnvironment
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
    private val outputPath: Path,
) : SiteBuilder {
    private fun Path.copyRecursively(target: Path) {
        Files.walk(this).forEach { source: Path ->
            val destination: Path = target.resolve(source.relativeTo(this))
            if (!destination.isDirectory()) {
                //avoid re-creating directories
                source.copyTo(destination, true)
            }
        }
    }

    override fun file(file: Path, remotePath: String) {
        val targetPath = outputPath.resolve(remotePath)
        if(file.isDirectory()){
            targetPath.parent.createDirectories()
            file.copyRecursively(targetPath)
        } else if(remotePath.isBlank()) {
            error("Can't mount file to an empty route")
        } else {
            targetPath.parent.createDirectories()
            file.copyTo(targetPath, true)
        }
    }

    override fun resourceFile(remotePath: String, resourcesPath: String) {
        val targetPath = outputPath.resolve(remotePath)
        targetPath.parent.createDirectories()
        javaClass.getResource(resourcesPath)?.let { Path.of(it.toURI()) }?.copyTo(targetPath, true)
    }

    override fun resourceDirectory(resourcesPath: String) {
        outputPath.parent.createDirectories()
        javaClass.getResource(resourcesPath)?.let { Path.of(it.toURI()) }?.copyRecursively(outputPath)
    }

    private fun resolveRef(baseUrl: String, ref: String) = if (baseUrl.isEmpty()) {
        ref
    } else if (ref.isEmpty()) {
        baseUrl
    } else {
        "${baseUrl.removeSuffix("/")}/$ref"
    }

    inner class StaticWebPage : WebPage {
        override val data: DataTree<*> get() = this@StaticSiteBuilder.data
        override val pageMeta: Meta get() = this@StaticSiteBuilder.siteMeta
        override val snark: SnarkHtmlPlugin get() = this@StaticSiteBuilder.snark


        override fun resolveRef(ref: String): String = resolveRef(baseUrl, ref)

        override fun resolvePageRef(pageName: Name): String = resolveRef(
            pageName.toWebPath() + ".html"
        )
    }


    override fun page(route: Name, content: context(WebPage, HTML) () -> Unit) {
        val htmlBuilder = createHTML()

        htmlBuilder.html {
            content(StaticWebPage(), this)
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
        metaOverride: Meta?,
        setAsRoot: Boolean,
    ): SiteBuilder = StaticSiteBuilder(
        snark = snark,
        data = dataOverride ?: data,
        siteMeta = metaOverride?.withDefault(siteMeta) ?: siteMeta,
        baseUrl = if (setAsRoot) {
            resolveRef(baseUrl, routeName.toWebPath())
        } else {
            baseUrl
        },
        outputPath = outputPath.resolve(routeName.toWebPath())
    )
}

/**
 * Create a static site using given [data] in provided [outputPath].
 * Use [siteUrl] as a base for all resolved URLs. By default, use [outputPath] absolute path as a base.
 *
 */
public fun SnarkEnvironment.static(
    outputPath: Path,
    siteUrl: String = outputPath.absolutePathString().replace("\\", "/"),
    block: SiteBuilder.() -> Unit,
) {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    val plugin = buildHtmlPlugin()
    StaticSiteBuilder(plugin, data, meta, siteUrl, outputPath).block()
}