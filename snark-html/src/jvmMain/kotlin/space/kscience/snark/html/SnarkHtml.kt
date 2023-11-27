@file:OptIn(DFExperimental::class)

package space.kscience.snark.html

import io.ktor.http.ContentType
import kotlinx.io.readByteArray
import space.kscience.dataforge.context.*
import space.kscience.dataforge.data.*
import space.kscience.dataforge.io.IOPlugin
import space.kscience.dataforge.io.IOReader
import space.kscience.dataforge.io.JsonMetaFormat
import space.kscience.dataforge.io.yaml.YamlMetaFormat
import space.kscience.dataforge.io.yaml.YamlPlugin
import space.kscience.dataforge.meta.*
import space.kscience.dataforge.misc.DFExperimental
import space.kscience.dataforge.names.*
import space.kscience.dataforge.provider.dfId
import space.kscience.dataforge.workspace.*
import space.kscience.snark.ImageIOReader
import space.kscience.snark.Snark
import space.kscience.snark.SnarkIOReader
import space.kscience.snark.TextProcessor
import java.net.URLConnection
import kotlin.io.path.Path
import kotlin.io.path.extension

public fun <T : Any> SnarkIOReader(
    reader: IOReader<T>,
    vararg types: ContentType,
    priority: Int = SnarkIOReader.DEFAULT_PRIORITY,
): SnarkIOReader<T> = SnarkIOReader(reader, types.map { it.toString() }.toSet(), priority)


/**
 * A plugin used for rendering a [DataTree] as HTML
 */
public class SnarkHtml : WorkspacePlugin() {
    private val snark by require(Snark)
    private val yaml by require(YamlPlugin)
    public val io: IOPlugin get() = snark.io

    override val tag: PluginTag get() = Companion.tag

    /**
     * Lazy-initialized variable that holds a map of site layouts.
     *
     * @property siteLayouts The map of site layouts, where the key is the layout name and the value is the corresponding SiteLayout object.
     */
    private val siteLayouts: Map<Name, SiteLayout> by lazy {
        context.gather(SiteLayout.TYPE, true)
    }


    internal fun siteLayout(layoutMeta: Meta): SiteLayout {
        val layoutName = layoutMeta.string
            ?: layoutMeta["name"].string ?: error("Layout name not defined in $layoutMeta")
        return siteLayouts[layoutName.parseAsName()] ?: error("Layout with name $layoutName not found in $this")
    }

    override fun content(target: String): Map<Name, Any> = when (target) {
        SnarkIOReader::class.dfId -> mapOf(
            "html".asName() to HtmlIOFormat.snarkReader,
            "markdown".asName() to MarkdownIOFormat.snarkReader,
            "json".asName() to SnarkIOReader(JsonMetaFormat, ContentType.Application.Json),
            "yaml".asName() to SnarkIOReader(YamlMetaFormat, "text/yaml"),
            "png".asName() to SnarkIOReader(ImageIOReader, ContentType.Image.PNG),
            "jpg".asName() to SnarkIOReader(ImageIOReader, ContentType.Image.JPEG),
            "gif".asName() to SnarkIOReader(ImageIOReader, ContentType.Image.GIF),
            "svg".asName() to SnarkIOReader(IOReader.binary, ContentType.Image.SVG, ContentType.parse("svg")),
            "raw".asName() to SnarkIOReader(
                IOReader.binary,
                "css",
                "js",
                "javascript",
                "scss",
                "woff",
                "woff2",
                "ttf",
                "eot"
            )
        )

        else -> super.content(target)
    }


    public val preprocess: TaskReference<String> by task<String> {
        pipeFrom<String,String>(dataByType<String>()) { text, _, meta ->
            meta[TextProcessor.TEXT_TRANSFORMATION_KEY]?.let {
                snark.textProcessor(it).process(text)
            } ?: text
        }
    }

    public val parse: TaskReference<Any> by task<Any> {
        from(preprocess).forEach { (dataName, data) ->
            //remove extensions for data files
            val filePath = meta[FileData.FILE_PATH_KEY]?.string ?: dataName.toString()
            val fileType = URLConnection.guessContentTypeFromName(filePath) ?: Path(filePath).extension
            val newName = dataName.replaceLast {
                if (fileType in setOf("md", "html", "yaml", "json")) {
                    NameToken(it.body.substringBeforeLast("."), it.index)
                } else {
                    it
                }
            }
            val parser = snark.readers.values.filter { parser ->
                fileType in parser.types
            }.maxByOrNull {
                it.priority
            } ?: run {
                logger.debug { "The parser is not found for file $filePath with meta $meta" }
                byteArraySnarkParser
            }
            data(newName, data.map { string: String ->
                parser.readFrom(string)
            })
        }
    }


//    public val textTransformationAction: Action<String, String> = Action.map<String, String> {
//        val transformations = actionMeta.getIndexed("transformation").entries.sortedBy {
//            it.key?.toIntOrNull() ?: 0
//        }.map { it.value }
//    }


    public companion object : PluginFactory<SnarkHtml> {
        override val tag: PluginTag = PluginTag("snark.html")

        override fun build(context: Context, meta: Meta): SnarkHtml = SnarkHtml()

        private val byteArrayIOReader = IOReader {
            readByteArray()
        }

        internal val byteArraySnarkParser = SnarkIOReader(byteArrayIOReader)

    }
}
