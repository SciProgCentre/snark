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


///**
// * Link-generating provider for Snark inline Markdown links.
// *
// * Extracts the link label, destination, and optional title from an inline link AST node and
// * converts supported Snark page-reference expressions in link destinations to internal
// * `snark://` destinations before normalizing them.
// *
// * Supported destination transformations include references to the home page, named references,
// * page references, and page metadata lookups. Destinations that do not match a supported Snark
// * reference expression are preserved and normalized as regular link destinations.
// *
// * @param baseURI base URI used by the underlying link generation mechanism for resolving links.
// * @param resolveAnchors whether anchor-only destinations should be resolved against the base URI.
// */
//private class SnarkInlineLinkGeneratingProvider(
//    baseURI: URI?,
//    resolveAnchors: Boolean = false,
//) : LinkGeneratingProvider(baseURI, resolveAnchors) {
//
//    override fun getRenderInfo(text: String, node: ASTNode): RenderInfo? {
//        return RenderInfo(
//            label = node.findChildOfType(MarkdownElementTypes.LINK_TEXT) ?: return null,
//            destination = node.findChildOfType(MarkdownElementTypes.LINK_DESTINATION)?.getTextInNode(text)?.let { raw ->
//                val processedLink = WebPageTextProcessor.functionRegex.replace(raw) { match ->
//                    when (match.groups["target"]?.value) {
//                        "homeRef" -> "snark://homeRef"
//                        "resolveRef" -> "snark://ref/${match.groups["name"]?.value ?: ""}"
//                        "resolvePageRef" -> "snark://page/${match.groups["name"]?.value ?: ""}"
//                        "pageMeta.get" -> "snark://meta/${match.groups["name"]?.value ?: ""}"
//                        else -> match.value
//                    }
//                }
//                LinkMap.normalizeDestination(processedLink, true)
//            } ?: "",
//            title = node.findChildOfType(MarkdownElementTypes.LINK_TITLE)?.getTextInNode(text)?.let {
//                LinkMap.normalizeTitle(it)
//            }
//        )
//    }
//}

//private class SnarkImageGeneratingProvider(
//    linkMap: LinkMap,
//    baseURI: URI?,
//) : ImageGeneratingProvider(linkMap, baseURI) {
//
//    val snarkInlineLinkProvider = SnarkInlineLinkGeneratingProvider(baseURI)
//    override fun getRenderInfo(text: String, node: ASTNode): RenderInfo? {
//        node.findChildOfType(MarkdownElementTypes.INLINE_LINK)?.let { linkNode ->
//            return snarkInlineLinkProvider.getRenderInfo(text, linkNode)
//        }
//        (node.findChildOfType(MarkdownElementTypes.FULL_REFERENCE_LINK)
//            ?: node.findChildOfType(MarkdownElementTypes.SHORT_REFERENCE_LINK))
//            ?.let { linkNode ->
//                return referenceLinkProvider.getRenderInfo(text, linkNode)
//            }
//        return null
//    }
//}

//public object SnarkFlavorDescriptor : CommonMarkFlavourDescriptor(false) {
//    override fun createHtmlGeneratingProviders(linkMap: LinkMap, baseURI: URI?): Map<IElementType, GeneratingProvider> =
//        super.createHtmlGeneratingProviders(linkMap, baseURI) + mapOf(
//            MarkdownElementTypes.INLINE_LINK to SnarkInlineLinkGeneratingProvider(baseURI, absolutizeAnchorLinks)
//                .makeXssSafe(useSafeLinks),
//            MarkdownElementTypes.IMAGE to SnarkImageGeneratingProvider(linkMap, baseURI)
//                .makeXssSafe(useSafeLinks),
//        )
//}

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
            "pageMeta.get" -> "snark://meta/${match.groups["name"]?.value ?: ""}"
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
        adapters(mathAdapter(), tablesAdapter())
    }

    override fun readFrom(source: String): PageFragment = PageFragment {
        val document = parser.parse(source)
        renderer.render(document, this)
    }

    override fun readFrom(source: Source): PageFragment = readFrom(source.readString())
}