package space.kscience.snark.html

import io.ktor.utils.io.core.readBytes
import space.kscience.dataforge.context.*
import space.kscience.dataforge.data.DataTree
import space.kscience.dataforge.io.*
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
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.typeOf

@PublishedApi
internal class SnarkParserWrapper<R : Any>(
    val reader: IOReader<R>,
    override val type: KType,
    override val fileExtensions: Set<String>,
) : SnarkParser<R> {
    override fun parse(context: Context, meta: Meta, bytes: ByteArray): R = bytes.asBinary().readWith(reader)
}

/**
 * Create a generic parser from reader
 */
@Suppress("FunctionName")
public inline fun <reified R : Any> SnarkParser(
    reader: IOReader<R>,
    vararg fileExtensions: String,
): SnarkParser<R> = SnarkParserWrapper(reader, typeOf<R>(), fileExtensions.toSet())

public class SnarkPlugin : AbstractPlugin() {
    private val yaml by require(YamlPlugin)
    public val io: IOPlugin get() = yaml.io

    override val tag: PluginTag get() = Companion.tag

    internal val parsers: Map<Name, SnarkParser<Any>> by lazy {
        context.gather(SnarkParser.TYPE, true)
    }

    private val siteLayouts: Map<Name, SiteLayout> by lazy {
        context.gather(SiteLayout.TYPE, true)
    }

    private val textTransformations: Map<Name, TextTransformation> by lazy {
        context.gather(TextTransformation.TYPE, true)
    }

    internal fun siteLayout(layoutMeta: Meta): SiteLayout {
        val layoutName = layoutMeta.string
            ?: layoutMeta["name"].string ?: error("Layout name not defined in $layoutMeta")
        return siteLayouts[layoutName.parseAsName()] ?: error("Layout with name $layoutName not found in $this")
    }

    internal fun textTransformation(transformationMeta: Meta): TextTransformation {
        val transformationName = transformationMeta.string
            ?: transformationMeta["name"].string ?: error("Transformation name not defined in $transformationMeta")
        return textTransformations[transformationName.parseAsName()]
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
        )
        TextTransformation.TYPE -> mapOf(
            "basic".asName() to BasicTextTransformation
        )
        else -> super.content(target)
    }

    public companion object : PluginFactory<SnarkPlugin> {
        override val tag: PluginTag = PluginTag("snark")
        override val type: KClass<out SnarkPlugin> = SnarkPlugin::class

        override fun build(context: Context, meta: Meta): SnarkPlugin = SnarkPlugin()

        private val byteArrayIOReader = IOReader {
            readBytes()
        }

        internal val byteArraySnarkParser = SnarkParser(byteArrayIOReader)
    }
}

@OptIn(DFExperimental::class)
public fun SnarkPlugin.readDirectory(path: Path): DataTree<Any> = io.readDataDirectory(path) { dataPath, meta ->
    val fileExtension = meta[FileData.META_FILE_EXTENSION_KEY].string ?: dataPath.extension
    val parser: SnarkParser<Any> = parsers.values.filter { parser ->
        fileExtension in parser.fileExtensions
    }.maxByOrNull {
        it.priority
    } ?: run {
        logger.warn { "The parser is not found for file $dataPath with meta $meta" }
        SnarkPlugin.byteArraySnarkParser
    }

    parser.reader(context, meta)
}