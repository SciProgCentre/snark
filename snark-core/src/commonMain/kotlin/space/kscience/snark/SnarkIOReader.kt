package space.kscience.snark

import space.kscience.dataforge.io.IOReader
import space.kscience.dataforge.io.asBinary
import space.kscience.dataforge.misc.DfId
import space.kscience.snark.SnarkIOReader.Companion.DEFAULT_PRIORITY
import space.kscience.snark.SnarkIOReader.Companion.DF_TYPE

@DfId(DF_TYPE)
public interface SnarkIOReader<out T>: IOReader<T> {
    public val types: Set<String>
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
 * @property types The set of supported types that can be read by the SnarkIOReader.
 * @property priority The priority of the SnarkIOReader. Higher priority SnarkIOReader instances will be preferred over lower priority ones.
 */

private class SnarkIOReaderWrapper<out T>(
    private val reader: IOReader<T>,
    override val types: Set<String>,
    override val priority: Int = DEFAULT_PRIORITY,
) : IOReader<T> by reader, SnarkIOReader<T> {

    override fun readFrom(source: String): T = readFrom(source.encodeToByteArray().asBinary())
}

public fun <T : Any> SnarkIOReader(
    reader: IOReader<T>,
    vararg types: String,
    priority: Int = DEFAULT_PRIORITY
): SnarkIOReader<T> = SnarkIOReaderWrapper(reader, types.toSet(), priority)