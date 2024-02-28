package space.kscience.snark.html

import space.kscience.dataforge.actions.AbstractAction
import space.kscience.dataforge.data.*
import space.kscience.dataforge.io.Binary
import space.kscience.dataforge.io.toByteArray
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.get
import space.kscience.snark.TextProcessor
import kotlin.reflect.typeOf

public class ParseAction(private val snarkHtml: SnarkHtml) :
    AbstractAction<Binary, PageFragment>(typeOf<PageFragment>()) {

    private fun parseOne(data: NamedData<Binary>): NamedData<PageFragment>? = with(snarkHtml) {
        val contentType = getContentType(data.name, data.meta)

        val parser = snark.readers.values.filterIsInstance<SnarkHtmlReader>().filter { parser ->
            contentType in parser.types
        }.maxByOrNull {
            it.priority
        }

        //ignore data for which parser is not found
        if (parser != null) {
            val preprocessor = meta[TextProcessor.TEXT_PREPROCESSOR_KEY]?.let { snark.preprocessor(it) }
            data.transform {
                if (preprocessor == null) {
                    parser.readFrom(it)
                } else {
                    //TODO provide encoding
                    val string = it.toByteArray().decodeToString()
                    parser.readFrom(preprocessor.process(string))
                }
            }.named(data.name)
        } else {
            null
        }
    }

    override fun DataSink<PageFragment>.generate(data: DataTree<Binary>, meta: Meta) {
        data.forEach {
            parseOne(it)?.let { put(it) }
        }
    }

    override fun DataSink<PageFragment>.update(source: DataTree<Binary>, meta: Meta, namedData: NamedData<Binary>) {
        parseOne(namedData)?.let { put(it) }
    }
}