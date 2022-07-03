package space.kscience.snark

import io.ktor.utils.io.core.Input
import io.ktor.utils.io.core.readBytes
import space.kscience.dataforge.context.Context
import space.kscience.dataforge.io.IOReader
import space.kscience.dataforge.io.asBinary
import space.kscience.dataforge.io.readWith
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.misc.Type
import kotlin.reflect.KType
import kotlin.reflect.typeOf

/**
 * A parser of binary content including priority flag and file extensions
 */
@Type(SnarkParser.TYPE)
public interface SnarkParser<out R> {
    public val type: KType

    public val fileExtensions: Set<String>

    public val priority: Int get() = DEFAULT_PRIORITY

    public fun parse(context: Context, meta: Meta, bytes: ByteArray): R

    public fun reader(context: Context, meta: Meta): IOReader<R> = object : IOReader<R> {
        override val type: KType get() = this@SnarkParser.type

        override fun readObject(input: Input): R = parse(context, meta, input.readBytes())
    }

    public companion object {
        public const val TYPE: String = "snark.parser"
        public const val DEFAULT_PRIORITY: Int = 10
    }
}

@PublishedApi
internal class SnarkParserWrapper<R : Any>(
    val reader: IOReader<R>,
    override val type: KType,
    override val fileExtensions: Set<String>,
) : SnarkParser<R> {
    override fun parse(context: Context, meta: Meta, bytes: ByteArray): R = bytes.asBinary().readWith(reader)
}

/**
 * Create a generic parser from reader
 */
@Suppress("FunctionName")
public inline fun <reified R : Any> SnarkParser(
    reader: IOReader<R>,
    vararg fileExtensions: String,
): SnarkParser<R> = SnarkParserWrapper(reader, typeOf<R>(), fileExtensions.toSet())