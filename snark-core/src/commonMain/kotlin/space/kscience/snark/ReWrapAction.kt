package space.kscience.snark

import space.kscience.dataforge.actions.AbstractAction
import space.kscience.dataforge.data.*
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.MutableMeta
import space.kscience.dataforge.meta.copy
import space.kscience.dataforge.names.*
import kotlin.reflect.KType
import kotlin.reflect.typeOf

/**
 * An action to change header (name and meta) without changing the data itself or its computation state
 */
public class ReWrapAction<R : Any>(
    type: KType,
    private val newMeta: MutableMeta.(name: Name) -> Unit = {},
    private val newName: (name: Name, meta: Meta?) -> Name,
) : AbstractAction<R, R>(type) {
    override fun DataSink<R>.generate(data: DataTree<R>, meta: Meta) {
        data.forEach { namedData ->
            put(
                newName(namedData.name, namedData.meta),
                namedData.data.withMeta(namedData.meta.copy { newMeta(namedData.name) })
            )
        }
    }

    override fun DataSink<R>.update(source: DataTree<R>, meta: Meta, namedData: NamedData<R>) {
        put(
            newName(namedData.name, namedData.meta),
            namedData.withMeta(namedData.meta.copy { newMeta(namedData.name) })
        )
    }

    public companion object {
        public inline fun <reified R : Any> removeExtensions(
            vararg bypassExtensions: String,
            noinline newMeta: MutableMeta.(name: Name) -> Unit = {},
        ): ReWrapAction<R> = ReWrapAction(typeOf<R>(), newMeta = newMeta) { name, _ ->
            name.replaceLast { token ->
                val extension = token.body.substringAfterLast('.')
                if (extension in bypassExtensions) {
                    NameToken(token.body.removeSuffix(".$extension"))
                } else {
                    token
                }
            }
        }

        public inline fun <reified R : Any> removeIndex(): ReWrapAction<R> = ReWrapAction<R>(typeOf<R>()) { name, _ ->
            if (name.endsWith("index")) name.cutLast() else name
        }
    }
}

public inline fun <reified R : Any> ReWrapAction(
    noinline newMeta: MutableMeta.(name: Name) -> Unit = {},
    noinline newName: (Name, Meta?) -> Name,
): ReWrapAction<R> = ReWrapAction(typeOf<R>(), newMeta, newName)

