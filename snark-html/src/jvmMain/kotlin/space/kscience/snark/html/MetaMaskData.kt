package space.kscience.snark.html

import space.kscience.dataforge.data.Data
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.MutableMeta
import space.kscience.dataforge.meta.copy


private class MetaMaskData<T>(val origin: Data<T>, override val meta: Meta) : Data<T> by origin

/**
 * A data with overriden meta. It reflects original data computed state.
 */
public fun <T> Data<T>.withMeta(newMeta: Meta): Data<T> = if (this is MetaMaskData) {
    MetaMaskData(origin, newMeta)
} else {
    MetaMaskData(this, newMeta)
}

public inline fun <T> Data<T>.mapMeta(block: MutableMeta.() -> Unit): Data<T> = withMeta(meta.copy(block))