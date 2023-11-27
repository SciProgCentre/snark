package space.kscience.snark.html

import io.ktor.http.ContentType
import kotlinx.html.div
import kotlinx.html.unsafe
import kotlinx.io.Source
import kotlinx.io.readString
import org.intellij.markdown.flavours.commonmark.CommonMarkFlavourDescriptor
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.MarkdownParser
import space.kscience.dataforge.io.IOReader
import space.kscience.snark.SnarkIOReader
import kotlin.reflect.KType
import kotlin.reflect.typeOf

public object HtmlIOFormat : IOReader<HtmlFragment> {
    override val type: KType = typeOf<HtmlFragment>()

    override fun readFrom(source: Source): HtmlFragment = HtmlFragment { page ->
        div {
            unsafe { +source.readString() }
        }
    }

    public val snarkReader: SnarkIOReader<HtmlFragment> = SnarkIOReader(this, ContentType.Text.Html)

}

public object MarkdownIOFormat : IOReader<HtmlFragment> {
    override val type: KType = typeOf<HtmlFragment>()

    private val markdownFlavor = CommonMarkFlavourDescriptor()
    private val markdownParser = MarkdownParser(markdownFlavor)

    override fun readFrom(source: Source): HtmlFragment = HtmlFragment { page ->
        val transformedText = source.readString()
        val parsedTree = markdownParser.buildMarkdownTreeFromString(transformedText)
        val htmlString = HtmlGenerator(transformedText, parsedTree, markdownFlavor).generateHtml()

        div {
            unsafe {
                +htmlString
            }
        }
    }

    public val snarkReader: SnarkIOReader<HtmlFragment> = SnarkIOReader(this, ContentType.parse("text/markdown"))

}

