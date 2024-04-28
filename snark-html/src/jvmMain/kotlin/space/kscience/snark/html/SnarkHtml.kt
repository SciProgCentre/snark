@file:OptIn(DFExperimental::class)

package space.kscience.snark.html

import io.ktor.http.ContentType
import kotlinx.coroutines.CoroutineScope
import kotlinx.io.readByteArray
import space.kscience.dataforge.actions.Action
import space.kscience.dataforge.actions.transform
import space.kscience.dataforge.context.Context
import space.kscience.dataforge.context.PluginFactory
import space.kscience.dataforge.context.PluginTag
import space.kscience.dataforge.data.*
import space.kscience.dataforge.io.Binary
import space.kscience.dataforge.io.IOPlugin
import space.kscience.dataforge.io.IOReader
import space.kscience.dataforge.io.JsonMetaFormat
import space.kscience.dataforge.io.yaml.YamlMetaFormat
import space.kscience.dataforge.io.yaml.YamlPlugin
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.get
import space.kscience.dataforge.meta.set
import space.kscience.dataforge.meta.string
import space.kscience.dataforge.misc.DFExperimental
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.asName
import space.kscience.dataforge.provider.dfType
import space.kscience.dataforge.workspace.*
import space.kscience.snark.ReWrapAction
import space.kscience.snark.Snark
import space.kscience.snark.SnarkReader
import space.kscience.snark.TextProcessor
import java.net.URLConnection
import kotlin.io.path.Path
import kotlin.io.path.extension


public fun <T : Any, R : Any> DataTree<T>.transform(action: Action<T, R>, meta: Meta = Meta.EMPTY): DataTree<R> =
    action.execute(this, meta)

/**
 * A plugin used for rendering a [DataTree] as HTML
 */
public class SnarkHtml : WorkspacePlugin() {
    public val snark: Snark by require(Snark)
    private val yaml by require(YamlPlugin)
    public val io: IOPlugin get() = snark.io

    override val tag: PluginTag get() = Companion.tag

    override fun content(target: String): Map<Name, Any> = when (target) {
        SnarkReader::class.dfType -> mapOf(
            "html".asName() to HtmlReader,
            "markdown".asName() to MarkdownReader,
            "json".asName() to SnarkReader(JsonMetaFormat, ContentType.Application.Json.toString()),
            "yaml".asName() to SnarkReader(YamlMetaFormat, "text/yaml", "yaml"),
        )

        else -> super.content(target)
    }

    internal fun getContentType(name: Name, meta: Meta): String = meta[CONTENT_TYPE_KEY].string ?: run {
        val filePath = meta[FileData.FILE_PATH_KEY]?.string ?: name.toString()
        URLConnection.guessContentTypeFromName(filePath) ?: Path(filePath).extension
    }

    public val prepareHeaderAction: ReWrapAction<Any> = ReWrapAction.removeExtensions<Any>("html", "md") { name ->
        val contentType = getContentType(name, this)
        set(CONTENT_TYPE_KEY, contentType)
    }

    public fun parse(name: Name, markup: String, meta: Meta): PageFragment {
        val contentType = getContentType(name, meta)

        val parser = snark.readers.values.filterIsInstance<SnarkHtmlReader>().filter { parser ->
            contentType in parser.types
        }.maxByOrNull {
            it.priority
        } ?: error("Parser for name $name and meta $meta not found")

        //ignore data for which parser is not found

        val preprocessor = meta[TextProcessor.TEXT_PREPROCESSOR_KEY]?.let { snark.preprocessor(it) }
        return if (preprocessor == null) {
            parser.readFrom(markup)
        } else {
            parser.readFrom(preprocessor.process(markup))
        }
    }


    public val removeIndexAction: ReWrapAction<Any> = ReWrapAction.removeIndex<Any>()

    public val parseAction: Action<Binary, PageFragment> = ParseAction(this)
    private val allDataNotNull: DataSelector<Any>
        get() = DataSelector { workspace, _ -> workspace.data.filterByType() }

    public val parse: TaskReference<Any> by task<Any>({
        description = "Parse all data for which reader is resolved"
    }) {
        //put all data
        putAll(from(allDataNotNull))
        //override parsed data
        putAll(from(allDataNotNull).filterByType<Binary>().transform(parseAction))
    }


    public companion object : PluginFactory<SnarkHtml> {
        override val tag: PluginTag = PluginTag("snark.html")

        public val CONTENT_TYPE_KEY: Name = "contentType".asName()

        override fun build(context: Context, meta: Meta): SnarkHtml = SnarkHtml()

        private val byteArrayIOReader = IOReader {
            readByteArray()
        }

        internal val byteArraySnarkParser = SnarkReader(byteArrayIOReader)

    }
}


public fun SnarkHtml.readSiteData(
    binaries: DataTree<Binary>,
    meta: Meta = Meta.EMPTY,
): DataTree<Any> = ObservableDataTree(context) {
    //put all binaries
    putAll(binaries)
    //override ones which could be parsed
    putAll(binaries.transform(parseAction, meta))
}.transform(prepareHeaderAction, meta).transform(removeIndexAction, meta)


public fun SnarkHtml.readSiteData(
    coroutineScope: CoroutineScope,
    meta: Meta = Meta.EMPTY,
    builder: DataSink<Binary>.() -> Unit,
): DataTree<Any> = readSiteData(ObservableDataTree(coroutineScope) { builder() }, meta)
