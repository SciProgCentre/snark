package space.kscience.snark.html

import io.heapy.kotmark.Extension
import io.heapy.kotmark.ext.gfm.tables.tables
import io.heapy.kotmark.ext.math.math
import io.heapy.kotmark.extensions
import io.heapy.kotmark.node.Image
import io.heapy.kotmark.node.Link
import io.heapy.kotmark.parser.Parser
import io.heapy.kotmark.parser.beta.LinkResult
import io.heapy.kotmark.parser.parser
import io.heapy.kotmark.renderer.kotlinxhtml.adapters.mathAdapter
import io.heapy.kotmark.renderer.kotlinxhtml.adapters.tablesAdapter
import io.heapy.kotmark.renderer.kotlinxhtml.kotlinxHtmlRenderer
import kotlinx.io.Source
import kotlinx.io.readString


/**
 * Parser extension that normalizes Snark-specific inline link targets during Markdown parsing.
 *
 * The extension registers a link processor that inspects the raw inline link destination and
 * rewrites recognized web-page reference expressions into internal `snark://` destinations.
 * Supported transformations include references to the home page, named resources, named pages,
 * and page metadata entries. Unrecognized link destinations are left unchanged.
 *
 * The original link title is preserved, and the processed destination is wrapped in a standard
 * Markdown link node for downstream rendering.
 *
 * Instances are created through [create].
 */
public class SnarkLinkExtension private constructor() : Parser.ParserExtension {

    private fun transformLink(raw: String) = WebPageTextProcessor.functionRegex.replace(raw) { match ->
        when (match.groups["target"]?.value) {
            "homeRef" -> "snark://homeRef"
            "resolveRef" -> "snark://ref/${match.groups["name"]?.value ?: ""}"
            "resolvePageRef" -> "snark://page/${match.groups["name"]?.value ?: ""}"
            "pageMeta.get" -> "snark://pageMeta/${match.groups["name"]?.value ?: ""}"
            else -> match.value
        }
    }

    override fun extend(parserConfiguration: Parser.Configuration) {
        parserConfiguration.linkProcessor { info, scanner, context ->
            val raw = info.text(scanner)
            val title = info.title()
            val destination = transformLink(raw)
            LinkResult.wrapTextIn(Link(destination, title), scanner.position())
        }
        parserConfiguration.postProcessor { node ->
            if (node is Image) {
                Image(node.destination?.let(::transformLink), node.title)
            } else {
                node
            }
        }
    }

    public companion object {
        @JvmStatic
        public fun create(): Extension = SnarkLinkExtension()
    }

}

public object MarkdownReader : SnarkHtmlReader {
    override val inputContentTypes: Set<String> = setOf("text/markdown", "md", "markdown")

    private val parser = parser {
        extensions {
            math()
            tables()
            add(SnarkLinkExtension.create())
        }
    }

    // Each syntax extension needs its matching adapter on the renderer side.
    private val renderer = kotlinxHtmlRenderer {
        escapeHtml = false
        sanitizeUrls = false
        adapters(mathAdapter(), tablesAdapter())
    }

    override fun readFrom(source: String): PageFragment = PageFragment {
        val document = parser.parse(source)
        renderer.render(document,  consumer)
    }

    override fun readFrom(source: Source): PageFragment = readFrom(source.readString())
}