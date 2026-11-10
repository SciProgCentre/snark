package space.kscience.snark.html

import kotlinx.html.*
import space.kscience.dataforge.meta.string
import space.kscience.dataforge.names.parseAsName
import space.kscience.snark.TextProcessor
import space.kscience.snark.html.language.resolveAssetRef
import java.net.URI

public class WebPageTextProcessor(private val page: PageContext) : TextProcessor {

    /**
     * A basic [TextProcessor] that replaces `${...}` expressions in text. The following expressions are recognised:
     * * `homeRef` resolves to [homeRef]
     * * `resolveRef("...")` -> [PageContext.resolveRef]
     * * `resolveAssetRef("...")` -> [PageContext.resolveAssetRef]
     * * `resolvePageRef("...")` -> [PageContext.resolvePageRef]
     * * `pageMeta.get("...")` -> [PageContext.pageMeta] get string method
     * Otherwise return unchanged string
     */
    override fun process(text: CharSequence): String = text.replace(functionRegex) { match ->
        when (match.groups["target"]?.value) {
            "homeRef" -> page.homeRef
            "resolveRef" -> {
                val refString = match.groups["name"]?.value ?: error("resolveRef requires a string (quoted) argument")
                page.resolveRef(refString)
            }

            "resolveAssetRef" -> {
                val refString = match.groups["name"]?.value
                    ?: error("resolveAssetRef requires a string (quoted) argument")
                page.resolveAssetRef(refString)
            }

            "resolvePageRef" -> {
                val refString = match.groups["name"]?.value
                    ?: error("resolvePageRef requires a string (quoted) argument")
                page.resolvePageRef(refString.parseAsName())
            }

            "pageMeta.get" -> {
                val nameString = match.groups["name"]?.value
                    ?: error("resolvePageRef requires a string (quoted) argument")
                page.pageMeta[nameString.parseAsName()].string ?: "@null"
            }

            "siteMeta.get" -> {
                val nameString = match.groups["name"]?.value
                    ?: error("resolvePageRef requires a string (quoted) argument")
                page.site.siteMeta[nameString.parseAsName()].string ?: "@null"
            }

            else -> match.value
        }
    }.replace(attributeRegex) { match ->
        val uri = URI(match.groups["uri"]!!.value)
        val path = uri.path.removePrefix("/")
        val snarkUrl = when (uri.authority) {
            "homeRef" -> page.homeRef
            "ref" -> page.resolveRef(path)
            "page" -> page.resolvePageRef(path.parseAsName())
            "pageMeta" -> page.pageMeta[path.parseAsName()].string ?: "@null"
            "siteMeta" -> page.site.siteMeta[path.parseAsName()].string ?: "@null"
            else -> match.value
        }
        snarkUrl
    }

    public companion object {
        internal val functionRegex = """\$\{(?<target>[\w.]*)(?:\((?:"|&quot;)(?<name>.*)(?:"|&quot;)\))?\}""".toRegex()
        private val attributeRegex = """(?<uri>snark://([^"]*))""".toRegex()
    }
}

/**
 * A tag consumer wrapper that wraps existing [TagConsumer] and adds postprocessing.
 *
 */
public class Postprocessor<out R>(
    public val page: PageContext,
    private val consumer: TagConsumer<R>,
    private val textProcessor: TextProcessor,
    private val linkProcessor: TextProcessor = textProcessor,
) : TagConsumer<R> by consumer {

    override fun onTagStart(tag: Tag) {
        if (tag.tagName == "code") codeDepth++
        val processedTag = when (tag.tagName) {
            "a" if tag.attributes["href"]?.startsWith("snark") == true -> {
                A(
                    initialAttributes = tag.attributes + ("href" to linkProcessor.process(tag.attributes["href"]!!)),
                    consumer = tag.consumer
                )
            }
            "img" if tag.attributes["src"]?.startsWith("snark") == true -> {
                IMG(
                    initialAttributes = tag.attributes + ("src" to linkProcessor.process(tag.attributes["src"]!!)),
                    consumer = tag.consumer
                )
            }
            else -> {
                tag
            }
        }
        consumer.onTagStart(processedTag)
    }

    // text inside <code> (code blocks and inline code) is left as is
    private var codeDepth = 0

    override fun onTagEnd(tag: Tag) {
        if (tag.tagName == "code") codeDepth--
        consumer.onTagEnd(tag)
    }

    /**
     * Text is HTML-escaped before processing, so a processor may insert markup (for example, numbered anchors).
     * Text without changes, and text inside `<code>`, is passed on unchanged.
     */
    override fun onTagContent(content: CharSequence) {
        if (codeDepth > 0) return consumer.onTagContent(content)
        val escaped = content.toString().replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        val processed = textProcessor.process(escaped)
        if (processed == escaped) {
            consumer.onTagContent(content)
        } else {
            consumer.onTagContentUnsafe { +processed }
        }
    }

    override fun onTagContentUnsafe(block: Unsafe.() -> Unit) {
        val proxy = object : Unsafe {
            override fun String.unaryPlus() {
                consumer.onTagContentUnsafe {
                    textProcessor.process(this@unaryPlus).unaryPlus()
                }
            }
        }
        proxy.block()
    }
}

//TODO replace postprocessor with markdown render plugin?

/**
 * Applies [processor] to text rendered in [block] and [linkProcessor] to `snark:` links in `href` and `src` attributes.
 */
context(page: PageContext)
public fun FlowContent.postprocess(
    processor: TextProcessor = WebPageTextProcessor(page),
    linkProcessor: TextProcessor = processor,
    block: context(PageContext) FlowContent.() -> Unit,
) {
    val fc = object : FlowContent by this {
        override val consumer: TagConsumer<*> = Postprocessor(page, this@postprocess.consumer, processor, linkProcessor)
    }

    fc.block()
}