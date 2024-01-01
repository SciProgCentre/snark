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

public object HtmlReader : SnarkReader<DataFragment> {
    override val types: Set<String> = setOf("html")

    override fun readFrom(source: String): DataFragment = DataFragment { _, _ ->
        div {
            unsafe { +source }
        }
    }

    override fun readFrom(source: Source): DataFragment = readFrom(source.readString())
    override val type: KType = typeOf<DataFragment>()
}

public object MarkdownReader : SnarkReader<DataFragment> {
    override val type: KType = typeOf<DataFragment>()

    override val types: Set<String> = setOf("text/markdown", "md", "markdown")

    override fun readFrom(source: String): DataFragment = DataFragment { _, _ ->
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

    override fun readFrom(source: Source): DataFragment = readFrom(source.readString())

    public val snarkReader: SnarkReader<DataFragment> = SnarkReader(this, "text/markdown")

}

