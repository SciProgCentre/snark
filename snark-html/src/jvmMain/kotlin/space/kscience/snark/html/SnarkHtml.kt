@file:OptIn(DFExperimental::class)

package space.kscience.snark.html

import io.ktor.http.ContentType
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.io.readByteArray
import space.kscience.dataforge.actions.Action
import space.kscience.dataforge.actions.mapping
import space.kscience.dataforge.actions.transform
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
import space.kscience.dataforge.misc.UnsafeKType
import space.kscience.dataforge.names.*
import space.kscience.dataforge.provider.dfType
import space.kscience.dataforge.workspace.*
import space.kscience.snark.*
import java.net.URLConnection
import kotlin.io.path.Path
import kotlin.io.path.extension
import kotlin.reflect.typeOf

context(snark: SnarkHtml)
public fun <T : Any, R : Any> DataTree<T>.transform(action: Action<T, R>, meta: Meta = Meta.EMPTY): DataTree<R> =
    action.execute(this, meta, snark.context)

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
            "html".asName() to RawHtmlReader,
            "markdown".asName() to MarkdownReader,
            "json".asName() to SnarkReader<Meta>(JsonMetaFormat, ContentType.Application.Json.toString()),
            "yaml".asName() to SnarkReader<Meta>(YamlMetaFormat, "text/yaml", "yaml"),
        )

        else -> super.content(target)
    }

    internal fun getContentType(name: Name, meta: Meta): String = meta[CONTENT_TYPE_KEY].string ?: run {
        val filePath = meta[FileDataTree.FILE_PATH_KEY]?.string ?: name.toString()
        URLConnection.guessContentTypeFromName(filePath) ?: Path(filePath).extension
    }

    internal val prepareHeaderAction: ReWrapAction<Any> = ReWrapAction(
        type = typeOf<Any>(),
        newMeta = { name ->
            val contentType = getContentType(name, this)
            set(FILE_NAME_KEY, name.last().toStringUnescaped())
            set(CONTENT_TYPE_KEY, contentType)
        }
    ) { name, _, type ->
        name.replaceLast { token ->
            val extension = token.body.substringAfterLast('.')
            if (type != typeOf<Binary>()) {
                NameToken(token.body.removeSuffix(".$extension"))
            } else {
                token
            }
        }
    }

    public val removeIndexAction: ReWrapAction<Any> = ReWrapAction(typeOf<Any>()) { name, _, _ ->
        if (name.endsWith("index")) name.cutLast() else name
    }

    public fun parseMarkup(name: Name, markup: String, meta: Meta): PageFragment {
        val contentType = getContentType(name, meta)

        val parser = snark.readers.values.filterIsInstance<SnarkHtmlReader>().filter { parser ->
            contentType in parser.inputContentTypes
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

    /**
     * Determines the content type for each item and searches for best suitable [SnarkReader] in the context to read the data.
     *
     * Of preprocessor node with key [TextProcessor.TEXT_PREPROCESSOR_KEY] id defined in meta, interpret data as text and preprocess it.
     *
     * Data without parsers is returned as raw [Binary].
     */
    public val parseAction: Action<Binary, Any> = Action.mapping {
        val contentType = getContentType(name, meta)

        val parser: SnarkReader<Any>? = snark.readers.values.filter { parser ->
            contentType in parser.inputContentTypes
        }.maxByOrNull {
            it.priority
        }

        @OptIn(UnsafeKType::class)
        result(parser?.outputType ?: typeOf<Binary>()) { data ->

            //ignore data for which parser is not found
            if (parser == null) {
                data
            } else {
                //apply text preprocessor if needed
                val preprocessor = meta[TextProcessor.TEXT_PREPROCESSOR_KEY]?.let {
                    snark.preprocessor(it)
                }
                //parse without preprocessing if no preprocessor is defined
                if (preprocessor == null) {
                    parser.readFrom(data)
                } else {
                    //if preprocessor is defined, read data as string and process it
                    //TODO provide encoding
                    val string = data.toByteArray().decodeToString()
                    parser.readFrom(preprocessor.process(string))
                }
            }
        }

    }
//
//    public val layoutAction: Action<Any, Any> = Action.mapping {
//
//    }

    private val allDataNotNull: DataSelector<Any>
        get() = DataSelector { workspace, _ -> workspace.data.filterByType() }

    /**
     * A task wrapping [parseAction].
     */
    public val parse: TaskReference<Any> by task<Any>({
        description = "Parse all data for which reader is resolved"
    }) {
        from(allDataNotNull).filterByType<Binary>().transform(parseAction, updateScope = context)
    }


    public companion object : PluginFactory<SnarkHtml> {
        override val tag: PluginTag = PluginTag("snark.html")

        public val FILE_NAME_KEY: Name = "contentType".asName()

        public val CONTENT_TYPE_KEY: Name = "contentType".asName()

        override fun build(context: Context, meta: Meta): SnarkHtml = SnarkHtml()

        private val byteArrayIOReader = IOReader { source ->
            source.readByteArray()
        }

        internal val byteArraySnarkParser = SnarkReader(byteArrayIOReader)

    }
}


/**
 * Temporary function until DataForge 0.10.3
 */
context(snark: SnarkHtml)
private suspend fun <T : Any> DataSink<T>.launchWriteFrom(
    source: DataTree<T>,
    prefix: Name = Name.EMPTY,
): Job {
    writeAll(source, prefix)
    return snark.context.launch {
        source.updates.collect {
            write(prefix + it, source.read(it))
        }
    }
}

/**
 * Parse raw data tree into html primitives
 */
context(snark: SnarkHtml)
public fun parseDataTree(
    binaries: DataTree<Binary>,
    meta: Meta = Meta.EMPTY,
    observe: Boolean = true,
): DataTree<Any> = DataTree<Any> {
    //put all binaries
    node(Name.EMPTY, binaries)
    //override ones which could be parsed
    if (observe) {
        observeNode(Name.EMPTY, snark.context, binaries.transform(snark.parseAction, meta))
    } else {
        node(Name.EMPTY, binaries.transform(snark.parseAction, meta))
    }
}.transform(snark.prepareHeaderAction, meta).transform(snark.removeIndexAction, meta)

///**
// * Read directory
// */
//context(snark: SnarkHtml)
//public suspend fun DataSink<Binary>.directory(path: Path, prefix: Name = Name.EMPTY, monitor: Boolean = true) {
//    if(monitor) {
//        launchWriteFrom(snark.io.readDirectory(path, true), prefix = prefix)
//    } else{
//        writeAll(prefix, snark.io.readDirectory(path, false))
//    }
//}

/**
 * Read the parsed data tree by providing [builder] for raw binary data tree
 */
context(snark: SnarkHtml)
public fun buildDataTree(
    meta: Meta = Meta.EMPTY,
    builder: DataTreeBuilder<Binary>.() -> Unit,
): DataTree<Any> = parseDataTree(DataTree<Binary>{ builder() }, meta)


context(snark: SnarkHtml)
public fun DataTreeBuilder<Binary>.resourceDirectory(
    resource: String,
    name: Name = resource.replace("/", ".").parseAsName(),
    classLoader: ClassLoader = Thread.currentThread().contextClassLoader,
) {
    snark.context.launch {
        node(name, snark.io.readResourceDirectory(resource, classLoader))
    }
}