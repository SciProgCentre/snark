package space.kscience.snark.html

import kotlinx.html.div
import kotlinx.html.unsafe
import kotlinx.io.Source
import kotlinx.io.readString
import space.kscience.snark.SnarkReader
import kotlin.reflect.KType
import kotlin.reflect.typeOf


public interface SnarkHtmlReader : SnarkReader<PageFragment>{
    override val outputType: KType get() = typeOf<PageFragment>()
}

public object RawHtmlReader : SnarkHtmlReader {
    override val inputContentTypes: Set<String> = setOf("text/html", "html")

    override fun readFrom(source: String): PageFragment = PageFragment {
        div {
            unsafe { +source }
        }
    }

    override fun readFrom(source: Source): PageFragment = readFrom(source.readString())
}

