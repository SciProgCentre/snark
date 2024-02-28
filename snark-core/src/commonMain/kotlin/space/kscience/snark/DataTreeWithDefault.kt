package space.kscience.snark

import space.kscience.dataforge.data.Data
import space.kscience.dataforge.data.DataTree
import space.kscience.dataforge.data.GenericDataTree
import space.kscience.dataforge.names.NameToken
import kotlin.reflect.KType

public class DataTreeWithDefault<T>(public val tree: DataTree<T>, val default: DataTree<T>) :
    DataTree<T> {
    override val dataType: KType get() = tree.dataType

    override val self: DataTreeWithDefault<T> get() = this

    override val data: Data<T>? get() = tree.data ?: default.data
    override val items: Map<NameToken, GenericDataTree<T, *>> get() = default.items + tree.items
}