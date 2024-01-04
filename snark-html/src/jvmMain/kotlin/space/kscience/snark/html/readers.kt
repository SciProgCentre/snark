package space.kscience.snark.html

import kotlinx.html.div
import kotlinx.html.unsafe
import kotlinx.io.Source
import kotlinx.io.readString
import org.intellij.markdown.flavours.commonmark.CommonMarkFlavourDescriptor
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.MarkdownParser
import space.kscience.snark.SnarkReader
import kotlin.reflect.KType
import kotlin.reflect.typeOf

public object HtmlReader : SnarkReader<PageFragment> {
    override val types: Set<String> = setOf("html")

    override fun readFrom(source: String): PageFragment = PageFragment {
        div {
            unsafe { +source }
        }
    }

    override fun readFrom(source: Source): PageFragment = readFrom(source.readString())
    override val type: KType = typeOf<PageFragment>()
}

public object MarkdownReader : SnarkReader<PageFragment> {
    override val type: KType = typeOf<PageFragment>()

    override val types: Set<String> = setOf("text/markdown", "md", "markdown")

    override fun readFrom(source: String): PageFragment = PageFragment {
        val parsedTree = markdownParser.buildMarkdownTreeFromString(source)
        val htmlString = HtmlGenerator(source, parsedTree, markdownFlavor).generateHtml()

        div {
            unsafe {
                +htmlString
            }
        }
    }

    private val markdownFlavor = CommonMarkFlavourDescriptor()
    private val markdownParser = MarkdownParser(markdownFlavor)

    override fun readFrom(source: Source): PageFragment = readFrom(source.readString())

    public val snarkReader: SnarkReader<PageFragment> = SnarkReader(this, "text/markdown")

}

