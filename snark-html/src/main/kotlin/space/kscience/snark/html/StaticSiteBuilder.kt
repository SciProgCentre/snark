package space.kscience.snark.html

import kotlinx.html.HTML
import kotlinx.html.html
import kotlinx.html.stream.createHTML
import space.kscience.dataforge.data.DataTree
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.withDefault
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.isEmpty
import java.nio.file.Files
import java.nio.file.Path
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.io.path.*


internal class StaticSiteBuilder(
    override val snark: SnarkPlugin,
    override val data: DataTree<*>,
    override val siteMeta: Meta,
    private val baseUrl: String,
    private val path: Path,
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

    override fun assetFile(remotePath: String, file: Path) {
        val targetPath = path.resolve(remotePath)
        targetPath.parent.createDirectories()
        file.copyTo(targetPath, true)
    }

    override fun assetDirectory(remotePath: String, directory: Path) {
        val targetPath = path.resolve(remotePath)
        targetPath.parent.createDirectories()
        directory.copyRecursively(targetPath)
    }

    override fun assetResourceFile(remotePath: String, resourcesPath: String) {
        val targetPath = path.resolve(remotePath)
        targetPath.parent.createDirectories()
        javaClass.getResource(resourcesPath)?.let { Path.of(it.toURI()) }?.copyTo(targetPath, true)
    }

    override fun assetResourceDirectory(resourcesPath: String) {
        path.parent.createDirectories()
        javaClass.getResource(resourcesPath)?.let { Path.of(it.toURI()) }?.copyRecursively(path)
    }

    private fun resolveRef(baseUrl: String, ref: String) = if (baseUrl.isEmpty()) {
        ref
    } else if (ref.isEmpty()) {
        baseUrl
    } else {
        "${baseUrl.removeSuffix("/")}/$ref"
    }

    inner class StaticPage : Page {
        override val data: DataTree<*> get() = this@StaticSiteBuilder.data
        override val pageMeta: Meta get() = this@StaticSiteBuilder.siteMeta
        override val snark: SnarkPlugin get() = this@StaticSiteBuilder.snark


        override fun resolveRef(ref: String): String = resolveRef(baseUrl, ref)

        override fun resolvePageRef(pageName: Name): String = resolveRef(
            pageName.toWebPath() + ".html"
        )
    }


    override fun page(route: Name, content: context(Page, HTML) () -> Unit) {
        val htmlBuilder = createHTML()

        htmlBuilder.html {
            content(StaticPage(), this)
        }

        val newPath = if (route.isEmpty()) {
            path.resolve("index.html")
        } else {
            path.resolve(route.toWebPath() + ".html")
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
        path = path.resolve(routeName.toWebPath())
    )
}

public fun SnarkPlugin.renderStatic(
    outputPath: Path,
    data: DataTree<*> = DataTree.empty(),
    siteUrl: String = outputPath.absolutePathString().replace("\\", "/"),
    block: SiteBuilder.() -> Unit,
) {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    StaticSiteBuilder(this, data, meta, siteUrl, outputPath).block()
}