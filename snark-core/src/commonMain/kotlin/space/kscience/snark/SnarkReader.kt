package space.kscience.snark

import space.kscience.dataforge.io.IOReader
import space.kscience.dataforge.io.asBinary
import space.kscience.dataforge.misc.DfType
import space.kscience.snark.SnarkReader.Companion.DEFAULT_PRIORITY
import space.kscience.snark.SnarkReader.Companion.DF_TYPE
import kotlin.reflect.KType
import kotlin.reflect.typeOf

/**
 * A reader interface for Snark data, providing methods to read data from a source and handle priority and MIME types.
 */
@DfType(DF_TYPE)
public interface SnarkReader<out T> : IOReader<T> {
    public val outputType: KType
    public val inputContentTypes: Set<String>
    public val priority: Int get() = DEFAULT_PRIORITY
    public fun readFrom(source: String): T

    public companion object {
        public const val DF_TYPE: String = "snark.reader"
        public const val DEFAULT_PRIORITY: Int = 10
    }
}

/**
 * A wrapper class for IOReader that adds priority and MIME type handling.
 *
 * @param T The type of data to be read by the IOReader.
 * @property reader The underlying IOReader instance used for reading data.
 * @property inputContentTypes The set of supported types that can be read by the SnarkIOReader.
 * @property priority The priority of the SnarkIOReader. Higher priority SnarkIOReader instances will be preferred over lower priority ones.
 */
private class SnarkReaderWrapper<out T>(
    private val reader: IOReader<T>,
    override val outputType: KType,
    override val inputContentTypes: Set<String>,
    override val priority: Int = DEFAULT_PRIORITY,
) : IOReader<T> by reader, SnarkReader<T> {

    override fun readFrom(source: String): T = readFrom(source.encodeToByteArray().asBinary())
}

/**
 * A factory function to create a SnarkReader wrapping IOReader that adds priority and MIME type handling.
 *
 * @param T The type of data to be read by the IOReader.
 * @param reader The underlying IOReader instance used for reading data.
 * @param inputContentTypes The set of supported types that can be read by the SnarkIOReader.
 * @param priority The priority of the SnarkIOReader. Higher priority SnarkIOReader instances will be preferred over lower priority ones.
 */
public fun <T : Any> SnarkReader(
    reader: IOReader<T>,
    outputType: KType,
    vararg inputContentTypes: String,
    priority: Int = DEFAULT_PRIORITY,
): SnarkReader<T> = SnarkReaderWrapper(reader, outputType, inputContentTypes.toSet(), priority)


/**
 * A factory function to create a SnarkReader wrapping IOReader that adds priority and MIME type handling.
 *
 * @param T The type of data to be read by the IOReader.
 * @param reader The underlying IOReader instance used for reading data.
 * @param inputContentTypes The set of supported types that can be read by the SnarkIOReader.
 * @param priority The priority of the SnarkIOReader. Higher priority SnarkIOReader instances will be preferred over lower priority ones.
 */
public inline fun <reified T : Any> SnarkReader(
    reader: IOReader<T>,
    vararg inputContentTypes: String,
    priority: Int = DEFAULT_PRIORITY,
): SnarkReader<T> = SnarkReader(reader, typeOf<T>(), inputContentTypes = inputContentTypes, priority)