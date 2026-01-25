package space.kscience.snark

import space.kscience.dataforge.actions.AbstractAction
import space.kscience.dataforge.data.*
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.MutableMeta
import space.kscience.dataforge.meta.copy
import space.kscience.dataforge.names.Name
import kotlin.reflect.KType
import kotlin.reflect.typeOf

/**
 * An action to change the header (name and meta) without changing the data itself or its computation state
 */
public class ReWrapAction<R : Any>(
    type: KType,
    private val newMeta: MutableMeta.(name: Name) -> Unit = {},
    private val newName: (name: Name, meta: Meta?, type: KType) -> Name,
) : AbstractAction<R, R>(type) {

    override fun DataBuilderScope<R>.generate(
        source: DataTree<R>,
        meta: Meta
    ): Map<Name, Data<R>> = buildMap {
        source.forEach { namedData: NamedData<R> ->
            put(
                newName(namedData.name, namedData.meta, namedData.type),
                namedData.withMeta(namedData.meta.copy { newMeta(namedData.name) })
            )
        }
    }

    override suspend fun DataSink<R>.update(
        source: DataTree<R>,
        actionMeta: Meta,
        updateName: Name
    ) {
        val namedData = source[updateName] ?: return
        write(
            newName(updateName, namedData.meta, namedData.type),
            namedData.withMeta(namedData.meta.copy { newMeta(updateName) })
        )
    }

    public companion object
}

public inline fun <reified R : Any> ReWrapAction(
    noinline newMeta: MutableMeta.(name: Name) -> Unit = {},
    noinline newName: (Name, Meta?, type: KType) -> Name,
): ReWrapAction<R> = ReWrapAction(typeOf<R>(), newMeta, newName)

