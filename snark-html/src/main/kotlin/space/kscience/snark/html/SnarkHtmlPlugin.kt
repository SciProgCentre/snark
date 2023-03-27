package space.kscience.snark.html

import io.ktor.utils.io.core.readBytes
import space.kscience.dataforge.context.*
import space.kscience.dataforge.data.DataTree
import space.kscience.dataforge.data.node
import space.kscience.dataforge.io.IOPlugin
import space.kscience.dataforge.io.IOReader
import space.kscience.dataforge.io.JsonMetaFormat
import space.kscience.dataforge.io.yaml.YamlMetaFormat
import space.kscience.dataforge.io.yaml.YamlPlugin
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.get
import space.kscience.dataforge.meta.string
import space.kscience.dataforge.misc.DFExperimental
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.asName
import space.kscience.dataforge.names.parseAsName
import space.kscience.dataforge.workspace.FileData
import space.kscience.dataforge.workspace.readDataDirectory
import space.kscience.snark.SnarkParser
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.toPath

/**
 * A plugin used for rendering a [DataTree] as HTML
 */
public class SnarkHtmlPlugin : AbstractPlugin() {
    private val yaml by require(YamlPlugin)
    public val io: IOPlugin get() = yaml.io

    override val tag: PluginTag get() = Companion.tag

    internal val parsers: Map<Name, SnarkParser<Any>> by lazy {
        context.gather(SnarkParser.TYPE, true)
    }

    private val siteLayouts: Map<Name, SiteLayout> by lazy {
        context.gather(SiteLayout.TYPE, true)
    }

    private val textProcessors: Map<Name, TextProcessor> by lazy {
        context.gather(TextProcessor.TYPE, true)
    }

    internal fun siteLayout(layoutMeta: Meta): SiteLayout {
        val layoutName = layoutMeta.string
            ?: layoutMeta["name"].string ?: error("Layout name not defined in $layoutMeta")
        return siteLayouts[layoutName.parseAsName()] ?: error("Layout with name $layoutName not found in $this")
    }

    internal fun textProcessor(transformationMeta: Meta): TextProcessor {
        val transformationName = transformationMeta.string
            ?: transformationMeta["name"].string ?: error("Transformation name not defined in $transformationMeta")
        return textProcessors[transformationName.parseAsName()]
            ?: error("Text transformation with name $transformationName not found in $this")
    }

    override fun content(target: String): Map<Name, Any> = when (target) {
        SnarkParser.TYPE -> mapOf(
            "html".asName() to SnarkHtmlParser,
            "markdown".asName() to SnarkMarkdownParser,
            "json".asName() to SnarkParser(JsonMetaFormat, "json"),
            "yaml".asName() to SnarkParser(YamlMetaFormat, "yaml", "yml"),
            "png".asName() to SnarkParser(ImageIOReader, "png"),
            "jpg".asName() to SnarkParser(ImageIOReader, "jpg", "jpeg"),
            "gif".asName() to SnarkParser(ImageIOReader, "gif"),
            "svg".asName() to SnarkParser(IOReader.binary, "svg"),
            "raw".asName() to SnarkParser(IOReader.binary, "css", "js", "scss", "woff", "woff2", "ttf", "eot")
        )

        TextProcessor.TYPE -> mapOf(
            "basic".asName() to BasicTextProcessor
        )

        else -> super.content(target)
    }

    public companion object : PluginFactory<SnarkHtmlPlugin> {
        override val tag: PluginTag = PluginTag("snark")

        override fun build(context: Context, meta: Meta): SnarkHtmlPlugin = SnarkHtmlPlugin()

        private val byteArrayIOReader = IOReader {
            readBytes()
        }

        internal val byteArraySnarkParser = SnarkParser(byteArrayIOReader)
    }
}

@OptIn(DFExperimental::class)
public fun SnarkHtmlPlugin.readDirectory(path: Path): DataTree<Any> = io.readDataDirectory(
    path,
    setOf("md", "html", "yaml", "json")
) { dataPath, meta ->
    val fileExtension = meta[FileData.FILE_EXTENSION_KEY].string ?: dataPath.extension
    val parser: SnarkParser<Any> = parsers.values.filter { parser ->
        fileExtension in parser.fileExtensions
    }.maxByOrNull {
        it.priority
    } ?: run {
        logger.debug { "The parser is not found for file $dataPath with meta $meta" }
        SnarkHtmlPlugin.byteArraySnarkParser
    }

    parser.asReader(context, meta)
}

public fun SnarkHtmlPlugin.readResources(
    vararg resources: String,
    classLoader: ClassLoader = Thread.currentThread().contextClassLoader,
): DataTree<Any> {
//    require(resource.isNotBlank()) {"Can't mount root resource tree as data root"}
    return DataTree {
        resources.forEach { resource ->
            val path = classLoader.getResource(resource)?.toURI()?.toPath() ?: error(
                "Resource with name $resource is not resolved"
            )
            node(resource, readDirectory(path))
        }
    }
}