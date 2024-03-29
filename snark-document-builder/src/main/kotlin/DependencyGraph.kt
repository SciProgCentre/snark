package documentBuilder

import kotlinx.coroutines.coroutineScope
import kotlin.collections.MutableList


public typealias FileName = String

/**
 * Node of dependency graph.
 *
 * One node represents one file and its dependencies
 *
 * @property mdAst - AST tree of current file.
 * @property dependencies - list of tail end adjacent to this node (dependencies of current file to be resolved).
 */
public data class DependencyGraphNode(
    val mdAst: MdAstRoot,
    val dependencies: List<DependencyGraphEdge>
)

/**
 * Interface of all dependency edges.
 */
public sealed interface DependencyGraphEdge {
    public fun visit(graphManager: GraphManager)
}

/**
 * Include dependency edge.
 *
 * @property parentNode - node inside AST tree, that is parent for dependent node.
 * @property dependentNode - dependent node, i.e. node of part of document with include commands
 * @property includeList - list of files to be included.
 */
public data class IncludeDependency(
    val parentNode: MdAstParent,
    val dependentNode: MdAstElement,
    val includeList: List<FileName>
) : DependencyGraphEdge {
    override fun visit(graphManager: GraphManager) {
        val parent = parentNode
        val childs: MutableList<MdAstElement> = mutableListOf()
        for (file in includeList) {
            graphManager.buildDocument(file)
            childs.addAll(graphManager.graph.nodes[file]!!.mdAst.children)
        }
        val elements: MutableList<MdAstElement> = parent.children.toMutableList()
        val index = parent.children.indexOf(dependentNode)
        elements.removeAt(index)
        elements.addAll(index, childs)
        parent.children = elements
    }
}

// parent - List<MdAstElement> -------------------------------------
//                |                                                  \
//                 \                                                  \
//                  |                                                  \
// dependentNode - MdAstElement                                         \
//                                                                       |
// List<FileName> -> List<MdAstRoot> --> List<List<MdAstElement>> ===> List<MdAstElement>

/**
 * Whole dependency graph.
 *
 * @property nodes - map of nodes, where you can find DependencyGraphNode of file by its name.
 */
public data class DependencyGraph(
    val nodes: Map<FileName, DependencyGraphNode>
)