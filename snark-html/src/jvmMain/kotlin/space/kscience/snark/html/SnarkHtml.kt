@file:OptIn(DFExperimental::class)

package space.kscience.snark.html

import io.ktor.http.ContentType
import kotlinx.coroutines.CoroutineScope
import kotlinx.io.readByteArray
import space.kscience.dataforge.actions.Action
import space.kscience.dataforge.actions.mapping
import space.kscience.dataforge.context.Context
import space.kscience.dataforge.context.PluginFactory
import space.kscience.dataforge.context.PluginTag
import space.kscience.dataforge.data.*
import space.kscience.dataforge.io.*
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


public fun <T : Any, R : Any> DataSet<T>.transform(action: Action<T, R>, meta: Meta = Meta.EMPTY): DataSet<R> =
    action.execute(this, meta)

public fun <T : Any> DataSetBuilder<T>.fill(dataSet: DataSet<T>) {
    node(Name.EMPTY, dataSet)
}

/**
 * A plugin used for rendering a [DataTree] as HTML
 */
public class SnarkHtml : WorkspacePlugin() {
    private val snark by require(Snark)
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

    private fun getContentType(name: Name, meta: Meta): String = meta[CONTENT_TYPE_KEY].string ?: run {
        val filePath = meta[FileData.FILE_PATH_KEY]?.string ?: name.toString()
        URLConnection.guessContentTypeFromName(filePath) ?: Path(filePath).extension
    }


    public val prepareHeaderAction: Action<Any, Any> = ReWrapAction.removeExtensions("html", "md") { name ->
        val contentType = getContentType(name, this)
        set(CONTENT_TYPE_KEY, contentType)
    }

    public val parseAction: Action<Any, Any> = Action.mapping {
        val contentType = getContentType(name, meta)

        val parser = snark.readers.values.filter { parser ->
            contentType in parser.types
        }.maxByOrNull {
            it.priority
        }
        //pass data for which parser is not found
        if (parser == null) {
            result { it }
        } else {

            val preprocessor = meta[TextProcessor.TEXT_PREPROCESSOR_KEY]?.let { snark.preprocessor(it) }
            result {

                when (it) {
                    is CharSequence -> {
                        val string = it.toString()
                        val preprocessed = preprocessor?.process(string) ?: string
                        parser.readFrom(preprocessed)
                    }

                    is Binary -> {
                        if (preprocessor == null) {
                            parser.readFrom(it)
                        } else {
                            //TODO provide encoding
                            val string = it.toByteArray().decodeToString()
                            parser.readFrom(preprocessor.process(string))
                        }
                    }

                    // bypass for non textual-data
                    else -> it
                }
            }
        }
    }

    public val parse: TaskReference<Any> by task<Any>({
        description = "Parse all data for which reader is resolved"
    }) {
        fill(from(allData).transform(parseAction, taskMeta))
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
    binaries: DataSource<Binary>,
    meta: Meta = Meta.EMPTY,
): DataSet<Any> = binaries.transform(parseAction, meta)


public fun SnarkHtml.readSiteData(
    coroutineScope: CoroutineScope,
    meta: Meta = Meta.EMPTY,
    builder: context(IOPlugin)  DataSourceBuilder<Any>.() -> Unit,
): DataSet<Any> = DataSource(coroutineScope) { builder(io, this) }
    .transform(prepareHeaderAction, meta)
    .transform(parseAction, meta)