package space.kscience.snark.html

import space.kscience.dataforge.actions.AbstractAction
import space.kscience.dataforge.data.*
import space.kscience.dataforge.io.Binary
import space.kscience.dataforge.io.toByteArray
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.get
import space.kscience.dataforge.misc.DFInternal
import space.kscience.snark.SnarkReader
import space.kscience.snark.TextProcessor
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.reflect.KType
import kotlin.reflect.typeOf

@OptIn(DFInternal::class)
internal fun <T, R> Data<T>.transform(
    type: KType,
    meta: Meta = this.meta,
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    block: suspend (T) -> R,
): Data<R> {
    val data = Data(type, meta, coroutineContext, listOf(this)) {
        block(await())
    }
    return data
}

public class ParseAction(private val snarkHtml: SnarkHtml) :
    AbstractAction<Binary, Any>(typeOf<PageFragment>()) {

    private fun parseOne(data: NamedData<Binary>): NamedData<Any>? = with(snarkHtml) {
        val contentType = getContentType(data.name, data.meta)

        val parser: SnarkReader<Any>? = snark.readers.values.filter { parser ->
            contentType in parser.inputContentTypes
        }.maxByOrNull {
            it.priority
        }

        //ignore data for which parser is not found
        if (parser != null) {
            val preprocessor = meta[TextProcessor.TEXT_PREPROCESSOR_KEY]?.let { snark.preprocessor(it) }
            data.transform(parser.outputType) {
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

    override fun DataSink<Any>.generate(data: DataTree<Binary>, meta: Meta) {
        data.forEach {
            parseOne(it)?.let { put(it) }
        }
    }

    override fun DataSink<Any>.update(source: DataTree<Binary>, meta: Meta, namedData: NamedData<Binary>) {
        parseOne(namedData)?.let { put(it) }
    }
}