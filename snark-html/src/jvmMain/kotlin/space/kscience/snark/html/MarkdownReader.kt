package space.kscience.snark.html

import io.heapy.kotmark.Extension
import io.heapy.kotmark.ext.gfm.tables.tables
import io.heapy.kotmark.ext.math.math
import io.heapy.kotmark.extensions
import io.heapy.kotmark.node.Image
import io.heapy.kotmark.node.Link
import io.heapy.kotmark.node.LinkReferenceDefinition
import io.heapy.kotmark.node.Text
import io.heapy.kotmark.parser.Parser
import io.heapy.kotmark.parser.beta.InlineContentParser
import io.heapy.kotmark.parser.beta.InlineContentParserFactory
import io.heapy.kotmark.parser.beta.InlineParserState
import io.heapy.kotmark.parser.beta.LinkResult
import io.heapy.kotmark.parser.beta.ParsedInline
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
        // Follows kotmark's CoreLinkProcessor: the destination is inline or taken from a reference definition,
        // and `!` marks an image. Links without a template are left to the core processor (null result).
        parserConfiguration.linkProcessor { info, scanner, context ->
            val definition = if (info.destination() == null) {
                context.getDefinition(
                    LinkReferenceDefinition::class,
                    info.label()?.takeIf { it.isNotEmpty() } ?: info.text(scanner)
                ) ?: return@linkProcessor null
            } else null
            val raw = definition?.destination ?: info.destination() ?: return@linkProcessor null
            val title = definition?.title ?: info.title()
            val destination = transformLink(raw)
            if (destination == raw) return@linkProcessor null
            if (info.marker()?.literal == "!") {
                LinkResult.wrapTextIn(Image(destination, title), scanner.position()).includeMarker()
            } else {
                LinkResult.wrapTextIn(Link(destination, title), scanner.position())
            }
        }
    }

    public companion object {
        @JvmStatic
        public fun create(): Extension = SnarkLinkExtension()
    }

}

/**
 * Keeps `${...}` template expressions as literal text, so that Markdown syntax (math, emphasis) inside or around them
 * does not break them before a postprocessor evaluates them. It must be registered before the math extension,
 * because both start with `$`.
 */
public class TemplateExpressionExtension private constructor() : Parser.ParserExtension {

    private class ExpressionParser : InlineContentParser {
        override fun tryParse(inlineParserState: InlineParserState): ParsedInline? {
            val scanner = inlineParserState.scanner()
            val start = scanner.position()
            if (!scanner.next("\${")) return null
            var depth = 1
            while (scanner.hasNext() && scanner.peek() != '\n') {
                when (scanner.peek()) {
                    '{' -> depth++
                    '}' -> if (--depth == 0) {
                        scanner.next()
                        return ParsedInline.of(Text(scanner.getSource(start, scanner.position()).content), scanner.position())
                    }
                }
                scanner.next()
            }
            return null
        }
    }

    private class Factory : InlineContentParserFactory {
        override val triggerCharacters: Set<Char> = setOf('$')
        override fun create(): InlineContentParser = ExpressionParser()
    }

    override fun extend(parserConfiguration: Parser.Configuration) {
        parserConfiguration.customInlineContentParserFactory(Factory())
    }

    public companion object {
        @JvmStatic
        public fun create(): Extension = TemplateExpressionExtension()
    }
}

public object MarkdownReader : SnarkHtmlReader {
    override val inputContentTypes: Set<String> = setOf("text/markdown", "md", "markdown")

    // `extensions { }` alone builds a list and drops it; it must be passed to `Configuration.extensions`.
    private val parser = parser {
        extensions(
            extensions {
                add(TemplateExpressionExtension.create())
                math()
                tables()
                add(SnarkLinkExtension.create())
            }
        )
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