package space.kscience.snark

import space.kscience.dataforge.actions.AbstractAction
import space.kscience.dataforge.data.*
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.MutableMeta
import space.kscience.dataforge.meta.copy
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.NameToken
import space.kscience.dataforge.names.replaceLast
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
    override fun DataSetBuilder<R>.generate(data: DataSet<R>, meta: Meta) {
        data.forEach { namedData ->
            data(
                newName(namedData.name, namedData.meta),
                namedData.data.withMeta(namedData.meta.copy { newMeta(namedData.name) })
            )
        }
    }

    override fun DataSourceBuilder<R>.update(dataSet: DataSet<R>, meta: Meta, updateKey: Name) {
        val datum = dataSet[updateKey]
        data(
            newName(updateKey, datum?.meta),
            datum?.withMeta(datum.meta.copy { newMeta(updateKey) })
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
    }
}

public inline fun <reified R : Any> ReWrapAction(
    noinline newMeta: MutableMeta.(name: Name) -> Unit = {},
    noinline newName: (Name, Meta?) -> Name
): ReWrapAction<R> =    ReWrapAction(typeOf<R>(), newMeta, newName)

