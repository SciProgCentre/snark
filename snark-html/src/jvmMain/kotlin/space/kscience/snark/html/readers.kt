package space.kscience.snark.html

import io.ktor.http.ContentType
import kotlinx.html.div
import kotlinx.html.unsafe
import kotlinx.io.Source
import kotlinx.io.readString
import org.intellij.markdown.flavours.commonmark.CommonMarkFlavourDescriptor
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.MarkdownParser
import space.kscience.snark.SnarkIOReader
import kotlin.reflect.KType
import kotlin.reflect.typeOf

public object HtmlReader : SnarkIOReader<HtmlFragment> {
    override val types: Set<String> = setOf("html")

    override fun readFrom(source: String): HtmlFragment = HtmlFragment {
        div {
            unsafe { +source }
        }
    }

    override fun readFrom(source: Source): HtmlFragment = readFrom(source.readString())
    override val type: KType = typeOf<HtmlFragment>()
}

public object MarkdownReader : SnarkIOReader<HtmlFragment> {
    override val type: KType = typeOf<HtmlFragment>()

    override val types: Set<String> = setOf("text/markdown", "md", "markdown")

    override fun readFrom(source: String): HtmlFragment = HtmlFragment {
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

    override fun readFrom(source: Source): HtmlFragment = readFrom(source.readString())

    public val snarkReader: SnarkIOReader<HtmlFragment> = SnarkIOReader(this, ContentType.parse("text/markdown"))

}

