package space.kscience.snark

import space.kscience.dataforge.io.IOReader
import space.kscience.dataforge.io.asBinary
import space.kscience.dataforge.misc.DfType
import space.kscience.snark.SnarkReader.Companion.DEFAULT_PRIORITY
import space.kscience.snark.SnarkReader.Companion.DF_TYPE

@DfType(DF_TYPE)
public interface SnarkReader<out T> : IOReader<T> {
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

private class SnarkReaderWrapper<out T>(
    private val reader: IOReader<T>,
    override val types: Set<String>,
    override val priority: Int = DEFAULT_PRIORITY,
) : IOReader<T> by reader, SnarkReader<T> {

    override fun readFrom(source: String): T = readFrom(source.encodeToByteArray().asBinary())
}

public fun <T : Any> SnarkReader(
    reader: IOReader<T>,
    vararg types: String,
    priority: Int = DEFAULT_PRIORITY,
): SnarkReader<T> = SnarkReaderWrapper(reader, types.toSet(), priority)