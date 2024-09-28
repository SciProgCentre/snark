package space.kscience.snark.html

import kotlinx.io.Source
import kotlinx.io.readString
import org.intellij.markdown.IElementType
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.findChildOfType
import org.intellij.markdown.ast.getTextInNode
import org.intellij.markdown.flavours.commonmark.CommonMarkFlavourDescriptor
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.flavours.space.SFMFlavourDescriptor
import org.intellij.markdown.html.*
import org.intellij.markdown.parser.LinkMap
import org.intellij.markdown.parser.MarkdownParser
import space.kscience.snark.SnarkReader

private class SnarkInlineLinkGeneratingProvider(
    baseURI: URI?,
    resolveAnchors: Boolean = false,
) : LinkGeneratingProvider(baseURI, resolveAnchors) {

    override fun getRenderInfo(text: String, node: ASTNode): RenderInfo? {
        return RenderInfo(
            label = node.findChildOfType(MarkdownElementTypes.LINK_TEXT) ?: return null,
            destination = node.findChildOfType(MarkdownElementTypes.LINK_DESTINATION)?.getTextInNode(text)?.let { raw ->
                val processedLink = WebPageTextProcessor.functionRegex.replace(raw) { match ->
                    when (match.groups["target"]?.value) {
                        "homeRef" -> "snark://homeRef"
                        "resolveRef" -> "snark://ref/${match.groups["name"]?.value ?: ""}"
                        "resolvePageRef" -> "snark://page/${match.groups["name"]?.value ?: ""}"
                        "pageMeta.get" -> "snark://meta/${match.groups["name"]?.value ?: ""}"
                        else -> match.value
                    }
                }
                LinkMap.normalizeDestination(processedLink, true)
            } ?: "",
            title = node.findChildOfType(MarkdownElementTypes.LINK_TITLE)?.getTextInNode(text)?.let {
                LinkMap.normalizeTitle(it)
            }
        )
    }
}

private class SnarkImageGeneratingProvider(
    linkMap: LinkMap,
    baseURI: URI?,
) : ImageGeneratingProvider(linkMap, baseURI) {

    val snarkInlineLinkProvider = SnarkInlineLinkGeneratingProvider(baseURI)
    override fun getRenderInfo(text: String, node: ASTNode): RenderInfo? {
        node.findChildOfType(MarkdownElementTypes.INLINE_LINK)?.let { linkNode ->
            return snarkInlineLinkProvider.getRenderInfo(text, linkNode)
        }
        (node.findChildOfType(MarkdownElementTypes.FULL_REFERENCE_LINK)
            ?: node.findChildOfType(MarkdownElementTypes.SHORT_REFERENCE_LINK))
            ?.let { linkNode ->
                return referenceLinkProvider.getRenderInfo(text, linkNode)
            }
        return null
    }
}

public object SnarkFlavorDescriptor : CommonMarkFlavourDescriptor(false) {
    override fun createHtmlGeneratingProviders(linkMap: LinkMap, baseURI: URI?): Map<IElementType, GeneratingProvider> =
        super.createHtmlGeneratingProviders(linkMap, baseURI) + mapOf(
            MarkdownElementTypes.INLINE_LINK to SnarkInlineLinkGeneratingProvider(baseURI, absolutizeAnchorLinks)
                .makeXssSafe(useSafeLinks),
            MarkdownElementTypes.IMAGE to SnarkImageGeneratingProvider(linkMap, baseURI)
                .makeXssSafe(useSafeLinks),
        )
}

public object MarkdownReader : SnarkHtmlReader {
    override val inputContentTypes: Set<String> = setOf("text/markdown", "md", "markdown")

    override fun readFrom(source: String): PageFragment = PageFragment {
        val parsedTree = markdownParser.parse(IElementType("ROOT"), source)
//        markdownParser.buildMarkdownTreeFromString(source)
        val htmlString = HtmlGenerator(source, parsedTree, markdownFlavor).generateHtml()

        consumer.onTagContentUnsafe {
            +htmlString
        }

    }

    private val markdownFlavor = SnarkFlavorDescriptor
    private val markdownParser = MarkdownParser(markdownFlavor)

    override fun readFrom(source: Source): PageFragment = readFrom(source.readString())
}