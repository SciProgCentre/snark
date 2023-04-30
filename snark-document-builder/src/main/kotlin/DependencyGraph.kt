package documentBuilder

sealed interface DependencyGraphEdge {
}
 
data class IncludeDependency(
    val parentNode: MdAstParent,
    val dependentNode: Iterator<MdAstElement>,
    val includeList: List<String>
) : DependencyGraphEdge
 
data class DependencyGraphNode(
    val mdAst: MdAstRoot,
    val dependencies: List<DependencyGraphEdge>
)
 
data class DependencyGraph(
    val nodes: Map<String, DependencyGraphNode>
)