package space.kscience.snark

import space.kscience.dataforge.data.DataSet
import space.kscience.dataforge.data.filterByType
import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.parseAsName
import space.kscience.dataforge.names.startsWith
import kotlin.reflect.typeOf

public inline fun <reified R : Any> DataSet<*>.branch(
    branchName: Name,
): DataSet<R> = filterByType(typeOf<R>()) { name, _ -> name.startsWith(branchName) }

public inline fun <reified R : Any> DataSet<*>.branch(
    branchName: String,
): DataSet<R> = filterByType(typeOf<R>()) { name, _ -> name.startsWith(branchName.parseAsName()) }