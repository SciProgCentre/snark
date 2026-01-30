package space.kscience.snark

import space.kscience.dataforge.context.ContextAware
import space.kscience.dataforge.data.DataTree
import space.kscience.dataforge.data.DataTreeBuilder
import space.kscience.dataforge.data.observeNode
import space.kscience.dataforge.names.Name

//@DslMarker
public annotation class SnarkBuilder

/**
 * A marker interface for Snark Page and Site builders
 */
@SnarkBuilder
public interface SnarkContext

context(contextAware: ContextAware)
public fun <T> DataTreeBuilder<T>.observeNode(prefix: Name, tree: DataTree<T>): Unit =
    observeNode(prefix, contextAware.context, tree)

context(contextAware: ContextAware)
public fun <T> DataTreeBuilder<T>.observeNode(prefix: String, tree: DataTree<T>): Unit =
    observeNode(prefix, contextAware.context, tree)