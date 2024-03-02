package space.kscience.snark.html

import kotlinx.html.div
import kotlinx.html.unsafe
import kotlinx.io.Source
import kotlinx.io.readString
import space.kscience.snark.SnarkReader


public interface SnarkHtmlReader : SnarkReader<PageFragment>

public object HtmlReader : SnarkHtmlReader {
    override val types: Set<String> = setOf("html")

    override fun readFrom(source: String): PageFragment = PageFragment {
        div {
            unsafe { +source }
        }
    }

    override fun readFrom(source: Source): PageFragment = readFrom(source.readString())
}

