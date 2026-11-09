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
        val snarkUrl = when (uri.authority) {
            "homeRef" -> page.homeRef
            "ref" -> page.resolveRef(uri.path)
            "page" -> page.resolvePageRef(uri.path.parseAsName())
            "pageMeta" -> page.pageMeta[uri.path.parseAsName()].string ?: "@null"
            "siteMeta" -> page.site.siteMeta[uri.path.parseAsName()].string ?: "@null"
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
) : TagConsumer<R> by consumer {

    override fun onTagStart(tag: Tag) {
        val processedTag = when (tag.tagName) {
            "a" if tag.attributes["href"]?.startsWith("snark") == true -> {
                A(
                    initialAttributes = tag.attributes + ("href" to textProcessor.process(tag.attributes["href"]!!)),
                    consumer = tag.consumer
                )
            }
            "img" if tag.attributes["src"]?.startsWith("snark") == true -> {
                IMG(
                    initialAttributes = tag.attributes + ("src" to textProcessor.process(tag.attributes["src"]!!)),
                    consumer = tag.consumer
                )
            }
            else -> {
                tag
            }
        }
        consumer.onTagStart(processedTag)
    }

    override fun onTagContent(content: CharSequence) {
        consumer.onTagContent(textProcessor.process(content))
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

context(page: PageContext)
public fun FlowContent.postprocess(
    processor: TextProcessor = WebPageTextProcessor(page),
    block: context(PageContext) FlowContent.() -> Unit,
) {
    val fc = object : FlowContent by this {
        override val consumer: TagConsumer<*> = Postprocessor(page, this@postprocess.consumer, processor)
    }

    fc.block()
}