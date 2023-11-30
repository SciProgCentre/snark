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
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.get
import space.kscience.dataforge.meta.string
import space.kscience.dataforge.misc.DFExperimental
import space.kscience.dataforge.names.*
import space.kscience.dataforge.provider.dfId
import space.kscience.dataforge.workspace.*
import space.kscience.snark.ImageIOReader
import space.kscience.snark.Snark
import space.kscience.snark.SnarkReader
import space.kscience.snark.TextProcessor
import java.net.URLConnection
import kotlin.io.path.Path
import kotlin.io.path.extension


/**
 * A plugin used for rendering a [DataTree] as HTML
 */
public class SnarkHtml : WorkspacePlugin() {
    private val snark by require(Snark)
    private val yaml by require(YamlPlugin)
    public val io: IOPlugin get() = snark.io

    override val tag: PluginTag get() = Companion.tag

    override fun content(target: String): Map<Name, Any> = when (target) {
        SnarkReader::class.dfId -> mapOf(
            "html".asName() to HtmlReader,
            "markdown".asName() to MarkdownReader,
            "json".asName() to SnarkReader(JsonMetaFormat, ContentType.Application.Json.toString()),
            "yaml".asName() to SnarkReader(YamlMetaFormat, "text/yaml", "yaml"),
            "png".asName() to SnarkReader(ImageIOReader, ContentType.Image.PNG.toString()),
            "jpg".asName() to SnarkReader(ImageIOReader, ContentType.Image.JPEG.toString()),
            "gif".asName() to SnarkReader(ImageIOReader, ContentType.Image.GIF.toString()),
            "svg".asName() to SnarkReader(IOReader.binary, ContentType.Image.SVG.toString(), "svg"),
            "raw".asName() to SnarkReader(
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
        pipeFrom<String, String>(dataByType<String>()) { text, _, meta ->
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


//    public val site by task<Any> {
//
//    }

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

        internal val byteArraySnarkParser = SnarkReader(byteArrayIOReader)

    }
}
