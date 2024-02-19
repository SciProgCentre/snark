package space.kscience.snark


import space.kscience.dataforge.data.DataSource
import space.kscience.dataforge.data.DataTree
import space.kscience.dataforge.data.filterByType
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.startsWith

public inline fun <reified R : Any> DataTree<*>.branch(
    branchName: Name,
): DataSource<R> = filterByType<R> { name, _, _ -> name.startsWith(branchName) }
