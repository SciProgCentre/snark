package space.kscience.snark

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.merge
import space.kscience.dataforge.data.Data
import space.kscience.dataforge.data.DataTree
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.NameToken
import kotlin.reflect.KType

public class DataTreeWithDefault<T>(
    public val tree: DataTree<T>,
    public val default: DataTree<T>
) : DataTree<T> {
    override val dataType: KType get() = tree.dataType

    override val updates: Flow<Name> = merge(tree.updates, default.updates)

    override val data: Data<T>? get() = tree.data ?: default.data

    private fun mergeItems(
        treeItems: Map<NameToken, DataTree<T>>,
        defaultItems: Map<NameToken, DataTree<T>>,
    ): Map<NameToken, DataTree<T>> {
        val mergedKeys = treeItems.keys + defaultItems.keys
        return mergedKeys.associateWith {
            val treeItem = treeItems[it]
            val defaultItem = defaultItems[it]
            when {
                treeItem == null -> defaultItem!!
                defaultItem == null -> treeItem
                else -> DataTreeWithDefault(treeItem, defaultItem)
            }
        }
    }

    override val items: Map<NameToken, DataTree<T>> get() = mergeItems(tree.items, default.items)
}